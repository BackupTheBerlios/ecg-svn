package de.fu_berlin.inf.focustracker.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;

import de.fu_berlin.inf.focustracker.EventDispatcher;
import de.fu_berlin.inf.focustracker.interaction.Action;
import de.fu_berlin.inf.focustracker.interaction.Interaction;
import de.fu_berlin.inf.focustracker.interaction.JavaElementHelper;
import de.fu_berlin.inf.focustracker.interaction.JavaInteraction;
import de.fu_berlin.inf.focustracker.interaction.Origin;
import de.fu_berlin.inf.focustracker.interaction.SystemInteraction;
import de.fu_berlin.inf.focustracker.util.Units;


public class InteractionRepository {

	private static final long INACTIVITY_OFFSET = 10 * Units.SECOND;
	
	private static InteractionRepository instance;
	private HashMap<IJavaElement, Element> elements = new HashMap<IJavaElement, Element>();
	private List<SystemInteraction> systemInteractions = new ArrayList<SystemInteraction>();
	private List<Interaction> allInteractions = new ArrayList<Interaction>();
	private IJavaElement lastVisitedJavaElement;
	
	private InteractionRepository() {};

	public static InteractionRepository getInstance() {
		if(instance == null) {
			instance = new InteractionRepository();
		}
		return instance;
	}
	
	
	public void add(Interaction aInteraction) {
		allInteractions.add(aInteraction);
		if (aInteraction instanceof JavaInteraction) {
			add((JavaInteraction)aInteraction);
		} else if(aInteraction instanceof SystemInteraction) {
			systemInteractions.add((SystemInteraction)aInteraction);
		}
	}
	
	private void add(JavaInteraction aJavaInteraction) {
		
		// ignore PackageFragment and PackageFragmentRoot
		if(aJavaInteraction.getJavaElement() instanceof PackageFragment ||
				aJavaInteraction.getJavaElement() instanceof PackageFragmentRoot) {
			return;
		}
//		System.out.println(aJavaInteraction);
		
		Element element = null;
		synchronized(elements) {
			element = elements.get(aJavaInteraction.getJavaElement());
			if(element == null) {
				element = new Element(aJavaInteraction.getJavaElement(), aJavaInteraction.getRating());
				elements.put(aJavaInteraction.getJavaElement(), element);
			}
			Interaction lastInteraction = getLastInteraction(aJavaInteraction.getJavaElement());
			aJavaInteraction.setLastInteraction(lastInteraction);
			element.getInteractions().add(aJavaInteraction);
			element.setRating(aJavaInteraction.getRating());
			recalculateRatings(element);
		}		
//		System.err.println(aJavaInteraction.getJavaElement().getClass().getName() + " ... " + JavaElementHelper.toString(aJavaInteraction.getJavaElement()));
		JavaInteraction interaction = null;
		try {
			if(element.getJavaElement() instanceof ICompilationUnit) {
				interaction = new JavaInteraction(Action.SELECTED, ((ICompilationUnit)element.getJavaElement()).getPackageDeclarations()[0], 1d, Origin.SYSTEM);
			} else if(!(element.getJavaElement() instanceof IPackageDeclaration) && !(element.getJavaElement() instanceof PackageFragment)){
				IJavaElement parent = JavaElementHelper.getCompilationUnit(element.getJavaElement()); 
				if (parent != null) {
					interaction = new JavaInteraction(Action.SELECTED, parent, 1d, Origin.SYSTEM);
				}
				
			}
			
			if(interaction != null) {
				if( EventDispatcher.isStarted()) {
					EventDispatcher.getInstance().notifyInteractionObserved(
							interaction
							);
				} else {
					add(interaction);
				}
			}
			
		} catch (Throwable e) {
			e.printStackTrace();
			
//		} catch (JavaModelException e) {
//			// TODO fix me
//			e.printStackTrace();
		}
		
		
		
//		lastVisitedJavaElement = aJavaInteraction.getJavaElement();
//		javaElements.get(aJavaInteraction.getJavaElement())
//		element.addInteraction(aJavaInteraction);
//		javaElements.add(element);
		
//		IType type = (IType)aJavaInteraction.getJavaElement().getAncestor(IJavaElement.TYPE);
//		if(type != null) {
//			System.err.println("######################### " + JavaModelUtil.getFullyQualifiedName(type));
//		} else {
//			System.err.println("type is null: " + aJavaInteraction.getJavaElement());
//		}
	}
	
	private void addQuiet(Element aElement, JavaInteraction aJavaInteraction) {
		Interaction lastInteraction = getLastInteraction(aJavaInteraction.getJavaElement());
		aJavaInteraction.setLastInteraction(lastInteraction);
		aElement.getInteractions().add(aJavaInteraction);
	}

	private void addRecalculatedInteraction(Element aElement, double aRating) {
		aElement.setRating(aRating);
		addQuiet(aElement, new JavaInteraction(Action.RECALCULATED, aElement.getJavaElement(), aRating, Origin.SYSTEM, "recalculated"));
	}
	
	@SuppressWarnings("unchecked")
	private void recalculateRatings(Element aElement) {
		
		if (aElement.getRating() == 0d) {
			return;
		}
		if (aElement.getRating() > 0d && aElement.getRating() < 1d) {
			// check if an element in the same elementclass has the rating 1 to reveal it
			for (Element element : getElementsWithRating()) {
				if (element != aElement && element.getRating() == 1d && !(element.getJavaElement() instanceof ICompilationUnit || element.getJavaElement() instanceof IPackageDeclaration)) {
					addRecalculatedInteraction(element, 0.5d);
				}
			}
		} else if (aElement.getJavaElement() instanceof ICompilationUnit)  {
			for (Element element : getElementsForClass(aElement.getJavaElement().getClass())) {
				if(element != aElement) {
					addRecalculatedInteraction(element, 0d);
				}
			}
		} else if (aElement.getJavaElement() instanceof IPackageDeclaration || aElement.getJavaElement() instanceof IPackageFragment) {
			for (Element element : getElementsForClass(new Class[] {IPackageDeclaration.class, IPackageFragment.class})) {
				if(element != aElement) {
					addRecalculatedInteraction(element, 0d);
				}
			}
		} else {
			for (Element element : getElementsWithRating()) {
				if (element != aElement && !(element.getJavaElement() instanceof ICompilationUnit || element.getJavaElement() instanceof IPackageDeclaration)) {
					addRecalculatedInteraction(element, 0d);
				}
			}
		}
	}

	public double getRating(IJavaElement aJavaElement) {
		Element element = elements.get(aJavaElement);
		if(element != null) {
			return element.getRating();
		}
		// not rated, yet
		return 0.0d;
	}
	
//	public double getLastScore(IJavaElement aJavaElement) {
//		Element element = elements.get(aJavaElement);
//		if(element != null) {
//			return element.getLastInteraction().getSeverity();
//		}
//		// not rated, yet
//		return 0.0d;
//	}
//	
	public JavaInteraction getLastInteraction(IJavaElement aJavaElement) {
		Element element = elements.get(aJavaElement);
		if(element != null) {
			return element.getLastInteraction();
		}
		return null;
	}
	
//	public IJavaElement[] getJavaElements() {
//		return elements.keySet().toArray(new IJavaElement[elements.keySet().size()]);
//	}

	public IJavaElement[] getRatedJavaElements() {
		List<IJavaElement> ratedElements = new ArrayList<IJavaElement>();
		for (IJavaElement element : elements.keySet()) {
			if(getRating(element) > 0d) {
				ratedElements.add(element);
			}
		}
		IJavaElement[] ret = ratedElements.toArray(new IJavaElement[ratedElements.size()]);
//		Arrays.sort(ret, new Comparator<Ijava>);
		
		return ret;
	}
	
	public HashMap<IJavaElement, Element> getElements() {
		return elements;
	}

	public List<Element> getElementsWithRating() {
		
		List<Element> elementsWithRating = new ArrayList<Element>();
		for (Element element : elements.values()) {
			if(element.getRating() > 0d) {
				elementsWithRating.add(element);
			}
		}
		return elementsWithRating;
	}
	
	public List<Element> getElementsForClass(Class<? extends IJavaElement>...aClasses) {
		
		List<Element> elementsForClass = new ArrayList<Element>();
		for (Element element : elements.values()) {
			 if (element.getRating() > 0d) {
				for (Class clazz : aClasses) {
//					if(element.getJavaElement().getClass().isAssignableFrom(clazz)) {
					if(clazz.isAssignableFrom(element.getJavaElement().getClass())) {
						elementsForClass.add(element);
						break;
					}
				}
			}
		}
		return elementsForClass;
	}
	
	
	
	public List<Interaction> getAllInteractions() {
		return allInteractions;
	}

	public IJavaElement getLastVisitedJavaElement() {
		return lastVisitedJavaElement;
	}
	
	public List<JavaInteraction> getAll(Class<? extends IJavaElement> aClazz) {
		List<JavaInteraction> list = new ArrayList<JavaInteraction>();
		for (Interaction interaction : allInteractions) {
			if (interaction instanceof JavaInteraction) {
				JavaInteraction javaInteraction = (JavaInteraction) interaction;
//				if (javaInteraction.getJavaElement().getClass().isAssignableFrom(aClazz)) {
				if (aClazz.isAssignableFrom(javaInteraction.getJavaElement().getClass())) {
					list.add(javaInteraction);
				}
			}
		}
		return list;
	}

	public int removeInteractions(long aOlderThan) {
		
		int removed = 0;
		for (Element element : elements.values()) {
			
			for (Iterator iter = element.getInteractions().iterator(); iter.hasNext();) {
				Interaction interaction = (Interaction) iter.next();
				// if the interaction is older than the given argument, remove it
				if(interaction.getDate().getTime() < aOlderThan) {
					iter.remove();
					removed++;
				}
			}
			// remove element, if it seems to be unimportant
			if(element.getInteractions().size() == 0 && element.getRating() < 0.01d) {
				elements.remove(element);
			}
		}
		
		for (Iterator<SystemInteraction> iter = systemInteractions.iterator(); iter.hasNext();) {
			SystemInteraction interaction = iter.next();
			if(interaction.getDate().getTime() < aOlderThan && interaction.isExported()) {
				iter.remove();
				removed++;
			}
		}
		
		return removed;
	}
	
	
	public void calculateInactivities() {
		for (Element element : elements.values()) {
			calculateInactivity(element);			
		}
	}
	
	/**
	 * after a specified offset time, the rating of an element should decrease 
	 * @param aElement
	 */
	public void calculateInactivity(Element aElement) {
		if(aElement.getRating() == 0d || aElement.getLastInteraction() == null) {
			return;
		}
		long currentTime = System.currentTimeMillis();
		long start = aElement.getLastInteraction().getDate().getTime() + INACTIVITY_OFFSET;
		if(start < currentTime) {
			double decValue = aElement.getLastInteraction().getRating() - ((currentTime - start) / Units.SECOND) * 0.005;
			if(decValue < 0d) {
				decValue = 0d;
			}
			aElement.setRating(decValue);
		}
		
	}

	public List<SystemInteraction> getSystemInteractions() {
		return systemInteractions;
	}	
	
}
