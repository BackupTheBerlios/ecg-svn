package org.electrocodeogram.cpc.store.local.xml.utils;


import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.electrocodeogram.cpc.core.api.data.special.IStatefulObject;
import org.electrocodeogram.cpc.core.utils.CoreStringUtils;


public class XMLUtils
{
	private static final Log log = LogFactory.getLog(XMLUtils.class);

	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	/**
	 * Takes an identifier and encodes it in a way which conforms to XML requirements.<br/>
	 * The returned string may be used as an attribute or element name.
	 * 
	 * @param identifier an identifier, i.e. as returned by {@link IStatefulObject#getPersistenceClassIdentifier()}, never null.
	 * @return escaped version which can legally be used as XML attribute or element name, never null.
	 */
	public static String escapeIdentifier(String identifier)
	{
		//TODO: implement me
		return identifier;
	}

	/**
	 * Takes an escaped identifier, as generated by {@link XMLUtils#escapeIdentifier(String)} and returns
	 * the original, unescaped version of the identifier.
	 * 
	 * @param identifier escaped identifier as returned by {@link XMLUtils#escapeIdentifier(String)}, never null.
	 * @return original, unescaped version of the identifier, never null.
	 */
	public static String unEscapeIdentifier(String identifier)
	{
		//TODO: implement me
		return identifier;
	}

	/**
	 * Takes an arbitrary string and encodes it in a way which allows its use as content of an XML element.<br/>
	 * The string is left unchanged, if it does not violate any XML requirements. Otherwise it is encapsulated
	 * in a CDATA element. 
	 * 
	 * @param data the string to escape, never null. 
	 * @return escaped version of the given string which is save for use as content of an XML element, never null.
	 */
	public static String escapeData(String data)
	{
		data = data.replace("&", "&amp;");
		data = data.replace("<", "&lt;");
		data = data.replace(">", "&gt;");

		return data;
	}

	/**
	 * Takes an escaped string, as generated by {@link XMLUtils#escapeData(String)} and returns its original form.
	 * 
	 * @param data escaped string as generated by {@link XMLUtils#escapeData(String)}, never null.
	 * @return original, unescaped string, never null.
	 */
	public static String unEscapeData(String data)
	{
		data = data.replace("&lt;", "<");
		data = data.replace("&gt;", ">");
		data = data.replace("&amp;", "&");

		return data;
	}

	/**
	 * Takes an object of a CPC supported data type and returns its string representation.<br/>
	 * In most cases this method will simply call {@link Object#toString()}.<br/>
	 * Special handling is implemented for dates.<br/>
	 * <br/>
	 * All return values are properly escaped. 
	 * 
	 * @param value the value to convert into an XML string value, may be NULL.
	 * @return the escaped string value of the given object, empty string if object is NULL.
	 */
	public static String mapToXMLValue(Comparable<? extends Object> value)
	{
		if (value == null)
			return "";

		if (value instanceof Date)
			return simpleDateFormat.format((Date) value);

		return escapeData(value.toString());
	}

	/**
	 * Takes a string and a type and tries to convert the string into the given type.
	 * 
	 * @param dataType the type to convert the string value to, never null.
	 * @param value the string value to convert, never null.
	 * @return converted value, guaranteed to be castable to <em>dataType</em>, never null.
	 * 
	 * @throws IllegalArgumentException if conversion into given type is not possible.
	 */
	public static Comparable<? extends Object> convertValueToType(Class<? extends Object> dataType, String value)
			throws IllegalArgumentException
	{
		if (log.isTraceEnabled())
			log.trace("convertValueToType() - dataType: " + dataType + ", value: " + value);
		assert (dataType != null && value != null);

		Comparable<? extends Object> result = null;

		try
		{
			if (dataType == String.class)
				result = value;

			else if (dataType == Integer.class)
				result = Integer.parseInt(value);

			else if (dataType == Long.class)
				result = Long.parseLong(value);

			else if (dataType == Float.class)
				result = Float.parseFloat(value);

			else if (dataType == Double.class)
				result = Double.parseDouble(value);

			else if (dataType == Boolean.class)
				result = Boolean.parseBoolean(value);

			else if (dataType == Date.class)
				result = simpleDateFormat.parse(value);
			else
				log.error("convertValueToType() - unsupported data type - dataType: " + dataType + ", value: " + value,
						new Throwable());

		}
		catch (Exception e)
		{
			log.error("convertValueToType() - error while converting value to type - dataType: " + dataType
					+ ", value: " + value + " - " + e, e);

			throw new IllegalArgumentException("error while converting value to type - dataType: " + dataType
					+ ", value: " + value + " - " + e, e);
		}

		if (log.isTraceEnabled())
			log.trace("convertValueToType() - result: "
					+ (result != null ? CoreStringUtils.truncateString(result.toString()) : "null"));

		return result;
	}
}
