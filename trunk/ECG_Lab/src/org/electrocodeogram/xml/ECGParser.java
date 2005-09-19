package org.electrocodeogram.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xerces.parsers.DOMParser;
import org.electrocodeogram.module.ModuleDescriptor;
import org.electrocodeogram.module.ModuleProperty;
import org.electrocodeogram.module.classloader.ModuleClassLoader;
import org.electrocodeogram.module.registry.ModuleSetupLoadException;
import org.electrocodeogram.module.setup.ModuleConfiguration;
import org.electrocodeogram.module.setup.ModuleSetup;
import org.electrocodeogram.msdt.MicroSensorDataType;
import org.electrocodeogram.msdt.MicroSensorDataTypeException;
import org.electrocodeogram.system.SystemRoot;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

/**
 * 
 */
public class ECGParser
{

	private static final String MODULE_PROPERTIES_XSD = "lib/ecg/module.properties.xsd";

	private static final String MODULE_SETUP_XSD = "lib/ecg/module.setup.xsd";

	private static Logger logger = Logger.getLogger("ECGParser");

	private static Document parseDocument(File file, String schemaLocation) throws SAXException, IOException
	{
		InputSource inputSource = null;

		// read the property file
		try
		{
			inputSource = new InputSource(new FileReader(file));
		}
		catch (FileNotFoundException e2)
		{

			// is checked before and should never happen

			// TODO : message

		}

		// create the XML parser
		DOMParser parser = new DOMParser();

		// set the parsing properties
		try
		{
			parser.setFeature("http://xml.org/sax/features/validation", true);
			parser.setFeature("http://apache.org/xml/features/validation/schema", true);
			parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
			parser.setFeature("http://apache.org/xml/features/validation/dynamic", true);

			// parse the property file against the "module.properties.xsd
			// XML schema
			parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", schemaLocation);

		}
		catch (SAXNotRecognizedException e1)
		{
			// is checked and schould never occure

			// TODO : message

		}
		catch (SAXNotSupportedException e1)
		{
			// is checked and schould never occure

			// TODO : message
		}

		// parse

		parser.setErrorHandler(new ErrorHandler()
		{

			public void warning(SAXParseException exception) throws SAXException
			{

				logger.log(Level.SEVERE, exception.getMessage());

				throw new SAXException(exception.getMessage());
			}

			public void error(SAXParseException exception) throws SAXException
			{
				logger.log(Level.SEVERE, exception.getMessage());

				throw new SAXException(exception.getMessage());

			}

			public void fatalError(SAXParseException exception) throws SAXException
			{
				logger.log(Level.SEVERE, exception.getMessage());

				throw new SAXException(exception.getMessage());

			}
		});

		parser.parse(inputSource);

		// get the XML document
		return parser.getDocument();

	}

	public static ModuleDescriptor parseAsModuleDescriptor(File file) throws ClassLoadingException, MicroSensorDataTypeException, PropertyException, SAXException, IOException, ModuleSetupLoadException
	{

		Document document = parseDocument(file, MODULE_PROPERTIES_XSD);

		return new ModuleDescriptor(
				getSingleNodeValue("id", document),
				getSingleNodeValue("name", document),
				getSingleNodeValue("provider-name", document),
				getSingleNodeValue("version", document),
				getClass(file.getParent(), getSingleNodeValue("class", document)),
				getSingleNodeValue("description", document),
				getModuleProperties(document),
				getMicroSensorDataTypes(document, file.getParent()));
	}

	public static ModuleSetup parseAsModuleSetup(File file) throws SAXException, IOException, ModuleSetupLoadException, PropertyException
	{
		Document document = parseDocument(file, MODULE_SETUP_XSD);

		logger.log(Level.INFO, "The module setup file seems to be valid.");

		ModuleSetup moduleSetup = new ModuleSetup();

		Node[] moduleNodes = getChildNodes(document.getDocumentElement(), "module");

		logger.log(Level.INFO, "Found " + moduleNodes.length + " stored modules in module setup file.");

		if (moduleNodes.length == 0)
		{
			throw new ModuleSetupLoadException(
					"The module setup is empty in file " + file.getAbsolutePath());
		}

		for (Node moduleNode : moduleNodes)
		{

			ModuleConfiguration moduleConfiguration;

			ArrayList<ModuleProperty> modulePropertyList = null;

			ArrayList<Integer> connectedToList = null;

			String moduleName;

			String fromClassId;

			Integer storedModuleId = null;

			logger.log(Level.INFO, "Going to load next module.");

			String idValue = getAttributeValue(moduleNode, "id");

			try
			{
				storedModuleId = new Integer(idValue);
			}
			catch (NumberFormatException e)
			{
				logger.log(Level.SEVERE, "Illegal value for attribute \"id\" for element \"module\".");

				throw new ModuleSetupLoadException(
						"Illegal value for attribute \"id\" for element \"module\".");
			}

			logger.log(Level.INFO, "Value for attribute \"id\" for element \"module\".");

			Node nameNode = getChildNode(moduleNode, "name");

			Node fromClassIdNode = getChildNode(moduleNode, "fromClassId");

			moduleName = getNodeValue(nameNode);

			fromClassId = getNodeValue(fromClassIdNode);

			connectedToList = new ArrayList<Integer>();

			Node connectedToNode = getChildNode(moduleNode, "connectedTo");

			if (isNode(connectedToNode, "connectedTo"))
			{
				Node[] idNodes = getChildNodes(connectedToNode, "id");

				for (Node idNode : idNodes)
				{

					String connectedToIdValue = getNodeValue(idNode);

					try
					{
						Integer id = new Integer(connectedToIdValue);

						connectedToList.add(id);

						logger.log(Level.INFO, "Module number " + storedModuleId + " is connected to module with id " + id);

					}
					catch (NumberFormatException e)
					{
						logger.log(Level.SEVERE, "Illegal value for element \"id\" for element \"connectedTo\" for module number " + storedModuleId);

						throw new ModuleSetupLoadException(
								"Illegal value for element \"id\" for element \"connectedTo\" for module number " + storedModuleId);
					}

				}

			}

			modulePropertyList = new ArrayList<ModuleProperty>();

			Node propertiesNode = getChildNode(moduleNode, "properties");

			if (isNode(propertiesNode, "properties"))
			{
				Node[] propertyNodes = getChildNodes(propertiesNode, "property");

				for (Node propertyNode : propertyNodes)
				{
					String propertyName = null;

					String propertyValue = null;

					Node propertyNameNode = getChildNode(propertyNode, "name");

					propertyName = getNodeValue(propertyNameNode);

					Node propertyValueNode = getChildNode(propertyNode, "value");

					propertyValue = getNodeValue(propertyValueNode);

					ModuleProperty moduleProperty = new ModuleProperty(
							propertyName, propertyValue, null);

					modulePropertyList.add(moduleProperty);
				}
			}
			else
			{
				modulePropertyList = null;
			}

			moduleConfiguration = new ModuleConfiguration(
					connectedToList.toArray(new Integer[0]), storedModuleId.intValue(),
					moduleName,
					modulePropertyList.toArray(new ModuleProperty[0]),
					fromClassId);

			moduleSetup.addModuleConfiguration(moduleConfiguration);
		}

		return moduleSetup;

	}

	private static ModuleProperty[] getModuleProperties(Document document) throws PropertyException
	{
		Node properties;
		try
		{
			properties = getChildNode(document.getDocumentElement(), "properties");
		}
		catch (PropertyException e1)
		{
			return null;
		}

		ArrayList<ModuleProperty> moduleProperties = null;

		Node[] propertyNodes = getChildNodes(properties, "property");

		moduleProperties = new ArrayList<ModuleProperty>();

		for (Node propertyNode : propertyNodes)
		{

			Node modulePropertyNameNode = getChildNode(propertyNode, "propertyName");

			Node modulePropertyTypeNode = getChildNode(propertyNode, "propertyType");

			Node modulePropertyValueNode = getChildNode(propertyNode, "propertyValue");

			String modulePropertyType = null;

			String modulePropertyName = null;

			try
			{

				modulePropertyName = getNodeValue(modulePropertyNameNode);

				modulePropertyType = getNodeValue(modulePropertyTypeNode);
			}
			catch (ModuleSetupLoadException e)
			{
				throw new PropertyException();
			}

			Class type = null;

			try
			{
				type = Class.forName(modulePropertyType);
			}
			catch (ClassNotFoundException e)
			{
				throw new PropertyException();
			}

			String modulePropertyValue = null;

			try
			{
				modulePropertyValue = getNodeValue(modulePropertyValueNode);
			}
			catch (PropertyException e)
			{

			}
			catch (ModuleSetupLoadException e)
			{

			}

			moduleProperties.add(new ModuleProperty(modulePropertyName,
					modulePropertyValue, type));

		}

		return moduleProperties.toArray(new ModuleProperty[0]);

	}

	private static Class getClass(String classPath, String className) throws ClassLoadingException
	{
		Class moduleClass;

		ModuleClassLoader.getInstance().addModuleClassPath(classPath);

		try
		{
			moduleClass = ModuleClassLoader.getInstance().loadClass(className);
		}
		catch (ClassNotFoundException e)
		{
			throw new ClassLoadingException();
		}

		if (moduleClass == null)
		{
			throw new ClassLoadingException();
		}

		return moduleClass;
	}

	private static String getSingleNodeValue(String nodeName, Document document) throws PropertyException, ModuleSetupLoadException
	{
		Node node = getChildNode(document.getDocumentElement(), nodeName);

		return getNodeValue(node);
	}

	private static MicroSensorDataType[] getMicroSensorDataTypes(Document document, String currentModuleDirectoryString) throws MicroSensorDataTypeException, PropertyException, ModuleSetupLoadException
	{
		Node microSensorDataTypesNode = null;

		try
		{
			microSensorDataTypesNode = getChildNode(document.getDocumentElement(), "microsensordatatypes");
		}
		catch (PropertyException e1)
		{
			return null;
		}

		ArrayList<MicroSensorDataType> microSensorDataTypes = null;

		Node[] msdtNodes = getChildNodes(microSensorDataTypesNode, "microsensordatatype");

		microSensorDataTypes = new ArrayList<MicroSensorDataType>();

		for (Node msdtNode : msdtNodes)
		{

			Node msdtNameNode = getChildNode(msdtNode, "msdtName");

			Node msdtFileNode = getChildNode(msdtNode, "msdtFile");

			String msdtName = getNodeValue(msdtNameNode);

			String msdtFileString = getNodeValue(msdtFileNode);

			File msdtFile = new File(
					currentModuleDirectoryString + File.separator + msdtFileString);

			try
			{
				microSensorDataTypes.add(SystemRoot.getSystemInstance().getSystemMsdtRegistry().parseMicroSensorDataType(msdtFile));
			}
			catch (MicroSensorDataTypeException e)
			{
				throw new MicroSensorDataTypeException("");

			}

		}

		return microSensorDataTypes.toArray(new MicroSensorDataType[0]);
	}

	private static Node[] getChildNodes(Node parentNode, String nodeName)
	{
		if (parentNode == null || nodeName == null)
		{
			return null;
		}

		ArrayList<Node> nodeList = new ArrayList<Node>();

		NodeList childNodes = parentNode.getChildNodes();

		if (childNodes == null || childNodes.getLength() == 0)
		{
			logger.log(Level.WARNING, "Node " + parentNode.getNodeName() + " does not have any childNodes.");
		}

		for (int i = 0; i < childNodes.getLength(); i++)
		{
			Node node = childNodes.item(i);

			if (node.getNodeName().equals(nodeName))
			{
				nodeList.add(node);
			}
		}

		return nodeList.toArray(new Node[0]);
	}

	private static Node getChildNode(Node parentNode, String nodeName) throws PropertyException
	{
		Node[] propertyNameNodes = getChildNodes(parentNode, nodeName);

		if (propertyNameNodes == null || propertyNameNodes.length != 1)
		{
			throw new PropertyException();
		}

		return propertyNameNodes[0];
	}

	private static String getNodeValue(Node node) throws PropertyException, ModuleSetupLoadException
	{
		String value = null;

		if (node.getFirstChild() == null)
		{
			throw new PropertyException();
		}

		value = node.getFirstChild().getNodeValue();

		if (value == null || value.equals(""))
		{
			logger.log(Level.INFO, "Missing value for element " + node.getNodeName());

			throw new ModuleSetupLoadException(
					"Missing value for element " + node.getNodeName());
		}

		logger.log(Level.INFO, "Found value " + value + " for element " + node.getNodeName());

		return value;
	}

	private static boolean isNode(Node node, String nodeName)
	{
		if (node == null)
		{
			return false;
		}

		if (!node.getNodeName().equals(nodeName))
		{
			logger.log(Level.SEVERE, "Misspelled element " + node.getNodeName() + ". Should be " + nodeName);

			return false;
		}

		return true;
	}

	private static String getAttributeValue(Node node, String attributeName) throws ModuleSetupLoadException
	{
		if (node == null)
		{
			return null;
		}

		if (node.getAttributes() == null || node.getAttributes().getLength() == 0)
		{
			logger.log(Level.SEVERE, "Attribute missing for element " + node.getNodeName());

			throw new ModuleSetupLoadException(
					"Attribute missing for element " + node.getNodeName());
		}

		Node attributeNode = node.getAttributes().getNamedItem(attributeName);

		if (attributeNode == null || attributeNode.getFirstChild() == null)
		{
			logger.log(Level.SEVERE, "Attribute value missing for element " + node.getNodeName());

			throw new ModuleSetupLoadException(
					"Attribute value missing for element " + node.getNodeName());
		}

		return attributeNode.getFirstChild().getNodeValue();
	}
}
