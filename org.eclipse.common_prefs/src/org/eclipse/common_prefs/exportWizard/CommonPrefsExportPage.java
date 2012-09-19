/*******************************************************************************
 * Copyright (c) 2011 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ericsson - initial API and implementation
 *******************************************************************************/

package org.eclipse.common_prefs.exportWizard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.common_prefs.StartupPlugin;
import org.eclipse.common_prefs.core.CommonPrefEPFResources;
import org.eclipse.common_prefs.core.CommonPrefsHelper;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.osgi.service.prefs.BackingStoreException;



/**
 * Wizard page appearing when selecting File->Export...:Common Preferences
 * 
 * @author Domenic Alessi
 *
 */
public class CommonPrefsExportPage extends WizardPage {
	
	private Tree imPrefTree;
	private Map<String, TreeItem> imNodeMap;
	private TreeMap<String, IEclipsePreferences> nodeMap;
	
	private Button imSelectAllButton;
	private Button imDeselectAllButton;
	
	private Button imFilterCommonButton;
	private Button imFilterDefaultButton;

	private Button addToCommonPrefsButton;
	
	private Label prefFileLabel;
	private Combo prefFileCombo;
	private Button prefBrowseButton;
	
	public CommonPrefsExportPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setDescription("Select Preferences to Export."); //NON-NLS-1
	}
	
	public void createControl(Composite parent) {
		// ======================================
		// Create new composite

		// Set F1 help
        // PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.IMPORT_PROJECT_SET_PAGE);
			
		Composite inner = new Composite(/*composite*/parent, SWT.NULL);
		inner.setLayoutData(new GridData(SWT.FILL));
		GridLayout layoutInner = new GridLayout(3, false);
		layoutInner.marginHeight = 0;
		layoutInner.marginWidth = 0;
		inner.setLayout(layoutInner);
		
		initializeDialogUnits(parent); // Not needed if including composite above
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		
		// ======================================
		// Pref tree group

		Group treeGroup = new Group(inner, SWT.NULL);
		treeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		GridLayout layoutTreeComp = new GridLayout(3, false);
		treeGroup.setLayout(layoutTreeComp);

		// ======================================
	    // Filter common preferences
		
		imFilterCommonButton = new Button(treeGroup, SWT.CHECK); 		
		imFilterCommonButton.setText("Filter out preferences with value same as in Common Preferences files"); 
		imFilterCommonButton.setSelection(true);
		GridData gdfcb = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gdfcb.horizontalSpan = 3;
		imFilterCommonButton.setLayoutData(gdfcb);
		imFilterCommonButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				updatePrefTree();
			}
		});	
		
		imFilterDefaultButton = new Button(treeGroup, SWT.CHECK); 		
		imFilterDefaultButton.setText("Filter out preferences with value same as default value"); 
		imFilterDefaultButton.setSelection(true);
		GridData gdfdb = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gdfdb.horizontalSpan = 3;
		imFilterDefaultButton.setLayoutData(gdfdb);
		imFilterDefaultButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				updatePrefTree();
			}
		});	
		
		imPrefTree = new Tree(treeGroup, SWT.MULTI | SWT.BORDER | SWT.CHECK);
		GridData gdibst = new GridData(SWT.FILL, SWT.FILL, true, true);
		gdibst.horizontalSpan = 3;
		imPrefTree.setLayoutData(gdibst);
		imPrefTree.setHeaderVisible(true);
		imPrefTree.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {				
				TreeItem item = (TreeItem) event.item;
				boolean isChecked = item.getChecked();
				
				TreeItem projectNode = CommonPrefsHelper.getPluginNode(item);
				projectNode.setChecked(isChecked);
				updateCheckAllState(isChecked, projectNode);
			}
		});
		imPrefTree.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				updateTreeSize();
			}
		});
		
		TreeColumn tc = new TreeColumn(imPrefTree, SWT.LEFT);
		tc.setText("Preference");
		String toolTip = "The preference key. A green icon indicates that value is same as default value or as\n";
		toolTip += "defined in an existing preference file, a red icon indicates the value is different.";
		tc.setToolTipText(toolTip);
		tc.setResizable(true);
		
		tc = new TreeColumn(imPrefTree, SWT.LEFT);
		tc.setText("Value");
		tc.setToolTipText("The value for the preference.");
		tc.setResizable(true);
		
		imSelectAllButton = new Button(treeGroup, SWT.PUSH);
		imSelectAllButton.setText("Select All"); 
		GridData gdsab = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gdsab.widthHint = Math.max(widthHint, imSelectAllButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		imSelectAllButton.setLayoutData(gdsab);
		imSelectAllButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				updateCheckAllState(true, null);
			}
		});	

		imDeselectAllButton = new Button(treeGroup, SWT.PUSH); 		
		imDeselectAllButton.setText("Deselect All"); 
		GridData gddsab = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		gddsab.horizontalSpan = 2;
		gddsab.widthHint = Math.max(widthHint, imDeselectAllButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		imDeselectAllButton.setLayoutData(gddsab);
		imDeselectAllButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				updateCheckAllState(false, null);
			}
		});	
		
		
		// ======================================
	    // Browse for save location	

		prefFileLabel = new Label(inner, SWT.LEFT);
		prefFileLabel.setText("To preference file:");
		prefFileLabel.setToolTipText("Specify a file where the selected preferences will be saved.");
		GridData gdpfl = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		prefFileLabel.setLayoutData(gdpfl);
		
		prefFileCombo = new Combo(inner, SWT.DROP_DOWN);
		GridData gdfc = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gdfc.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		prefFileCombo.setLayoutData(gdfc);
		prefFileCombo.setItems(CommonPrefsFileHistory.getHistory());
		prefFileCombo.setText(CommonPrefsFileHistory.getSuggestedDefault());
		prefFileCombo.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				updateFinishEnablement();
			}
		});

		prefBrowseButton = new Button(inner, SWT.PUSH);
		prefBrowseButton.setText("Browse..." /*TeamUIMessages.ImportProjectSetMainPage_Browse_3*/); 
		GridData gdbb = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		gdbb.widthHint = Math.max(widthHint, prefBrowseButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		prefBrowseButton.setLayoutData(gdbb);
		prefBrowseButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {

				FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
				fd.setText("Please provide a filename for saving the preferences");
				fd.setFilterExtensions(new String[] {"*.epf", "*"}); //$NON-NLS-1$
				fd.setFilterNames(new String[] {"Preference Files (*.epf)", "All Files (*.*)"});
				
				String fileName = prefFileCombo.getText();
				if (fileName != null && fileName.length() > 0) {
					int separator= fileName.lastIndexOf(File.separatorChar); //$NON-NLS-1$
					if (separator != -1) {
						fileName= fileName.substring(0, separator);
					}
				} else {
					fileName= ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
				}
				fd.setFilterPath(fileName);
				
				String f = fd.open();
				if (f != null) {
					// Add suffix if missing
					if (!f.endsWith(".epf"))
						f += ".epf";
					
					prefFileCombo.setText(f);
					updateFinishEnablement();		
				}
			}
		});		

		addToCommonPrefsButton = new Button(inner, SWT.CHECK);
		addToCommonPrefsButton.setText("Include the preference file in the Common Preferences list");
		String cpToolTip = "Add the preference file to the list of preferences read at startup.\n";
		cpToolTip += "See also dialog in Windows:Preferences...:General:Common Preferences.";
		addToCommonPrefsButton.setToolTipText(cpToolTip);
		addToCommonPrefsButton.setSelection(false);
		GridData gdfb = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		gdfb.horizontalSpan = 3;
		addToCommonPrefsButton.setLayoutData(gdfb);		
		
		setControl(inner);
		
		// Initialize the page
		updatePreferencePage();
		
        Dialog.applyDialogFont(parent);		
	}
	
	private void updatePreferencePage() {
		// Reset the status message in top of wizard page
		setMessage(null);
		
		updateFinishEnablement();
		updatePrefTree();
	}
	
	private void updatePrefTree() {
		imPrefTree.removeAll();
		imNodeMap = new HashMap<String, TreeItem>();
		
		boolean filterCommon = imFilterCommonButton.getSelection();
		boolean filterDefault = imFilterDefaultButton.getSelection();
		
		// Temporary comment this code for testing with eclipse 3.5
		/*Preferences commonPrefs = null;*/
		// End
		
		// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
		Properties commonProperties = null;
		// END
		
		if (filterCommon || filterDefault) {
			// Temporary comment this code for testing with eclipse 3.5
			/*commonPrefs = CommonPrefsHelper.readPreferences(filterCommon, filterDefault);*/
			// End
			
			// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
			commonProperties = CommonPrefsHelper.readPreferences(filterCommon, filterDefault);
			// END
			
		}
		
		nodeMap = new TreeMap<String, IEclipsePreferences>();
		CommonPrefsHelper.sortPreferences(nodeMap);
		
		Collection<IEclipsePreferences> nodes = nodeMap.values();
		for (IEclipsePreferences node : nodes) {	
			// Temporary comment this code for testing with eclipse 3.5
	    	/*addNode(node, commonPrefs);*/
	    	// End
			
			// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
			addNode(node, commonProperties);
			// END
	    }
			
/*		
 		// TODO: To get more information on a plug-in and possible group in features
 		// tried to look up info. Not yet completed.
 
		IExtensionRegistry extReg = Platform.getExtensionRegistry();
		String[] names = extReg.getNamespaces();
		int i = 0;
		for (String name : names) {
			TreeItem bsItem = new TreeItem(imBuildSpecTree, SWT.NONE);
			bsItem.setText(name + "  " + (new Integer(i)).toString());
			i++;
		}		
		Bundle[] bundles = Platform.getBundles("*", "");
		int i = 0;
		for (Bundle name : bundles) {
			TreeItem bsItem = new TreeItem(imBuildSpecTree, SWT.NONE);
			bsItem.setText(name.getSymbolicName() + "  " + (new Integer(i)).toString());
			i++;
		}
*/		
	}
	
	/**
	 * Add a node and it's keys. If the commonPrefs are defined filter values.
	 * 
	 * @param node
	 * @param nodeName
	 * @param commonPrefs
	 */
	/*private void addNode(IEclipsePreferences node, Preferences commonPrefs) {*/
	private void addNode(IEclipsePreferences node, Properties commonPrefsProperties) {
		
    	// Get the actual preference key/value pairs
    	String[] keys = null;
		try {
			keys = node.keys();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		
		// If not any keys skip adding the entry. Note that the node
		// might have sub-nodes as well, and if needed we will re-add
		// any missing parent nodes
		if (keys == null || keys.length == 0)
			return;			
		
		// Add keys to TreeSet to order (and filter) them. NOTE: Since we don't want to
		// save individual keys, but rather a consistent set for the whole node - if one
		// is changed, include all. BUT give visual cue (icons) that not all are changed.
		String nodeName = node.absolutePath();
		
		TreeSet<String> keySetAll = new TreeSet<String>();
		TreeSet<String> keySetDiff = new TreeSet<String>();
		TreeSet<String> keySetDiffFile = new TreeSet<String>();
		for (String key : keys) {
			keySetAll.add(key);
			// Temporary comment this code for testing with eclipse 3.5
			/*if (commonPrefs != null){
				if (commonPrefs.contains(nodeName + "/" + key)) {
					String valueStr = node.get(key, "");
					String commStr = commonPrefs.getString(nodeName + "/" + key);
					if (commStr.compareTo(valueStr) != 0)
						keySetDiffFile.add(key); // Diff value than in file
					continue;
				}
			}	*/
			// End
			
			// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
			if (commonPrefsProperties != null){
				if (commonPrefsProperties.containsKey(nodeName + "/" + key)) {
					String valueStr = node.get(key, "");
					String commStr = commonPrefsProperties.getProperty(nodeName + "/" + key);
					if (commStr.compareTo(valueStr) != 0)
						keySetDiffFile.add(key); // Diff value than in file
					continue;
				}
			}
			// END
			keySetDiff.add(key);
		}

		// If not any keys has changed value skip adding the node
		if (keySetDiff.size() == 0 && keySetDiffFile.size() == 0)
			return;	
		
		// Get parent node from map 
		TreeItem pNode = CommonPrefsHelper.addNodeInternal(node, imNodeMap, imPrefTree);
		if (pNode == null)
			return; // Error
		
		ImageRegistry reg = StartupPlugin.getDefault().getImageRegistry();
		
		// Add key/value pairs to the tree
		for (String key : keySetAll) {
			TreeItem pdItem = new TreeItem(pNode, SWT.NONE);
			pdItem.setGrayed(true);
			pdItem.setText(key);
			pdItem.setText(1, node.get(key, "<default>"));
			
			// Indicate if the value is changed compared to what is defined in the Common Preferences
			// and the default values as saved on first init of workspace
			if (keySetDiff.contains(key))
				pdItem.setImage(reg.get(StartupPlugin.EXP_TREE_DIFF_VAL_IMG));
			else if (keySetDiffFile.contains(key))
				pdItem.setImage(reg.get(StartupPlugin.EXP_TREE_DIFF_FILE_VAL_IMG));
			else
				pdItem.setImage(reg.get(StartupPlugin.EXP_TREE_SAME_VAL_IMG));
		}		
	}
	
	/**
	 * The method will toggle the check state for all elements in the tree
	 * Note that this method will not trigger events, so also need to set 
	 * the item state.
	 * 
	 * @param isChecked
	 */
	private void updateCheckAllState(boolean isChecked, TreeItem parentItem){
		TreeItem[] bsItems = null;
		if (parentItem == null)
			bsItems = imPrefTree.getItems();
		else
			bsItems = parentItem.getItems();
		for (TreeItem bsItem : bsItems) {
			bsItem.setChecked(isChecked);
			updateCheckAllState(isChecked, bsItem);
		}
	}
	
	/**
	 * Compute size of table relative its bounds.
	 * TODO: Handle also resize within table.
	 */
	private void updateTreeSize() {
		TreeColumn[] columns = imPrefTree.getColumns();
		int w = imPrefTree.getSize().x;
		int width = (w * 60) / 100;
		columns[0].setWidth(width);
		
		// NOTE: Make sum width - 4 since avoids the scroll bar
		columns[1].setWidth(w - width - 4);	
	}

	private void updateFinishEnablement() {
		setMessage(null);
		
		if (prefFileCombo.getText().length() == 0) {
			setPageComplete(false);
			return;
		}
		
		File file = new File(prefFileCombo.getText());
		if (!file.isAbsolute()) {
			setMessage("Filename is not correct", ERROR);
			setPageComplete(false);
			return;
		} else if (file.isDirectory()) {
			setMessage("Filename is a directory", ERROR);
			setPageComplete(false);
			return;			
		}
		
		setPageComplete(true);
	}
	
	/**
	 * Export all selected entries to selected file. Note that each plug-in node
	 * is exported separately, so gives some overhead - i.e. multiple time stamps
	 * etc. Added a delimiter line to make file easier for human reading ...
	 * 
	 * @return boolean
	 */
	public boolean performFinish() {
		File file = getPreferenceFileName();
		if (file == null)
			return false;
		
		IPreferencesService service = Platform.getPreferencesService();
		OutputStream output = null;
		FileOutputStream fos = null;
		IStatus s = null;
		try {
			// Comment this code for testing with eclipse 3.5
			/*fos = new FileOutputStream(file);
			output = new BufferedOutputStream(fos);*/
			// End
			
			// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
			IPreferenceFilter[] filters = getPreferenceFilters();
			fos = new FileOutputStream(file);
			// END
			
			List<TreeItem> items = CommonPrefsHelper.getPluginNodes(imPrefTree);
			for (TreeItem treeItem : items) {
				if (treeItem.getChecked()) {
					IEclipsePreferences node = (IEclipsePreferences) treeItem.getData();	
					try {
						// Comment this code for testing with eclipse 3.5
						/*service.exportPreferences(node, output, (String[]) null);
						output.write("#===================================\n".getBytes());*/
						// End
						
						// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
						service.exportPreferences(node, filters, fos);
						fos.write("#===================================\n".getBytes());
						// END
					} catch (CoreException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}					
				}
			}			
		
			// Comment this code for testing with eclipse 3.5
			/*output.flush();
			fos.getFD().sync();*/
			// End
			
			// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
			fos.flush();
			fos.getFD().sync();
			// 
		} catch (IOException e) {
			e.printStackTrace();
			s = new Status(
					IStatus.ERROR,
					StartupPlugin.PLUGIN_ID,
					"Error when exporting preferences.",
					e);			
		} finally {
			// Comment this code for testing with eclipse 3.5
			/*if (output != null)
				try {
					output.close();
				} catch (IOException e) {
				}*/
			// End
				
			// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
				}	
			// END
		}
		
		if (s != null && !s.isOK()){
			ErrorDialog.openError(
					getShell(),
					"Export Wizard Failed",
					"One or more errors ocurred when trying to export the selected preferences.",
					s);			
		} else {
			// Remember the location of the latest successful import
			CommonPrefsFileHistory.remember(file.getPath());
			
			if (addToCommonPrefsButton.getSelection()) {
				// Add to the Common Preferences list as "init" entry
				CommonPrefEPFResources cpFiles = StartupPlugin.getDefault().getCommonPrefFiles(null);
				try {
					cpFiles.addPrefFile(null, file.getPath(), false, false);
				} catch (Exception e) {
					// Will throw Exception if not valid. Never mind here
				}
				cpFiles.save();
			}
		}		
		
		return true;
	}
	
	private File getPreferenceFileName() {
		String fileName = prefFileCombo.getText();	
		
		// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
		//System.out.println("EXPORT PAGE file name: " + fileName + "\n");
		// END
		
		File file = new File(fileName);
		if (file.exists()) {
			boolean okToOverwrite = MessageDialog.openConfirm(
					getShell(),
					"OK to overwrite file?",
					"The file " + fileName + " already exists. OK to overwrite?");
			if (okToOverwrite)
				file.delete();
			else
				return null;
		} 
		
		File parFile = file.getParentFile();
		if (parFile == null)
			return null;
		parFile.mkdirs();
		
		return file;
	}
	
	
	
	/**
	 * ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
	 * 
	 * @return  IPreferenceFilter
	 */
	private IPreferenceFilter[] getPreferenceFilters() {
		
		IPreferenceFilter[] transfers = new IPreferenceFilter[1];
		transfers[0] = new IPreferenceFilter() {
				public String[] getScopes() {
					return new String[] {InstanceScope.SCOPE,
							ConfigurationScope.SCOPE };
				}
				@SuppressWarnings("unchecked")
				public Map getMapping(String scope) {
					return null;
				}
		};	
		return transfers;
	}
	
}
