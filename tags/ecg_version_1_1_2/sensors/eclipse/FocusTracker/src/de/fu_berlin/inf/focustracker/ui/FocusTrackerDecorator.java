package de.fu_berlin.inf.focustracker.ui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import de.fu_berlin.inf.focustracker.EventDispatcher;
import de.fu_berlin.inf.focustracker.interaction.Interaction;
import de.fu_berlin.inf.focustracker.interaction.InteractionListener;
import de.fu_berlin.inf.focustracker.repository.InteractionRepository;

public class FocusTrackerDecorator extends LabelProvider implements ILightweightLabelDecorator, InteractionListener {

	public static final String ID = "de.fu_berlin.inf.focustracker.ui.FocusTrackerDecorator"; 
	
	public static final Font BOLD = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
	
	private DecimalFormat decimalFormat = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US)); 
	
//	public static final Color COLOR_0 = new Color(Display.getDefault(), 200, 10, 30);
//	public static final Color COLOR_1 = new Color(Display.getDefault(), 200, 100, 30);

	public FocusTrackerDecorator() {
		// must use asyncExec, because otherwise the listener is added to soon.
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				EventDispatcher.getInstance().addListener(FocusTrackerDecorator.this);
			}
		});
	}

	public void decorate(Object element, IDecoration decoration) {
		if(element instanceof IJavaElement) {
//			System.err.println("decorate " + ((IJavaElement)element).getElementName());
			double lastScore = InteractionRepository.getInstance().getRating((IJavaElement)element);
			if(lastScore == 0) {
				return;
			}
			if(lastScore > 0.5d) {
				decoration.setFont(BOLD);
			}
			decoration.addSuffix(" [" + decimalFormat.format(lastScore) + "]");
		}
	}

//	public void addListener(ILabelProviderListener listener) {
//		// don't care about listeners
//		System.err.println(" FocusTrackerDecorator listener added! " + listener.getClass().getName());
//	}

	public void dispose() {
		EventDispatcher.getInstance().removeListener(this);
	}

	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

//	public void removeListener(ILabelProviderListener listener) {
//		// don't care about listeners
//	}

	public synchronized static void refresh() {
//		Display.getDefault().syncExec(new Runnable() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				PlatformUI.getWorkbench().getDecoratorManager().update(FocusTrackerDecorator.ID);
			}
		});
	}

	public void notifyInteractionObserved(List<? extends Interaction> aInteractions) {
//		refresh();
//		List<IJavaElement> elements = new ArrayList<IJavaElement>();
//		for (Interaction interaction : aInteractions) {
//			if (interaction instanceof JavaInteraction) {
//				JavaInteraction javaInteraction = (JavaInteraction) interaction;
//				elements.add(javaInteraction.getJavaElement());
//			}
//		}
//		postLabelEvent(new LabelProviderChangedEvent(FocusTrackerDecorator.this, elements.toArray()));
		postLabelEvent(new LabelProviderChangedEvent(FocusTrackerDecorator.this, InteractionRepository.getInstance().getElements().keySet().toArray()));
//		postLabelEvent(new LabelProviderChangedEvent(FocusTrackerDecorator.this));
		
	}
	
	/**
	 * Post the label event to the UI thread
	 *
	 * @param events  the events to post
	 */
	private void postLabelEvent(final LabelProviderChangedEvent event) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				fireLabelProviderChanged(event);
			}
		});
	}
	
//	public static boolean isDecoratorActivated() {
//		return FocusTrackerPlugin.getDefault().getPluginPreferences().getBoolean(PreferenceConstants.P_DECORATOR_ACTIVATED);
//	}
	
	
}