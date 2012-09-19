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

package org.eclipse.common_prefs.preferences;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.common_prefs.PreferenceInitializer;
import org.eclipse.common_prefs.StartupPlugin;
import org.eclipse.common_prefs.core.CommonPrefEPFResource;
import org.eclipse.common_prefs.core.CommonPrefEPFResources;
import org.eclipse.common_prefs.core.CommonPrefsHelper;
import org.eclipse.common_prefs.preferences.PrefFileDialog.PrefFileDialogType;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.BackingStoreException;



/**
 * Page appearing in the Windows->Preferences dialog. Handling Common Preferences
 * settings.
 * Added code for fixing network issue in eclipse 3.5
 * 
 * @author Domenic Alessi
 *
 */
public class CommonPrefPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage {

	private TabFolder prefFolder;
	
	private Label prefGenInfoText;
	private Button prefLoadButton;
	private Table prefPathTable;
	private Button prefFileUpButton;
	private Button prefFileDownButton;
	private Button prefFileEditButton;
	private Button prefFileAddButton;
	private Button prefFileRemoveButton;
	private Button prefFileLoadButton;
	private Combo prefFileTypeCombo;
	
	private Label prefCompInfoText;
	private Table prefComparePathTable;
	private Tree imPrefTree;
	private Button prefCompareCurrentButton;
	private boolean isComparePageInvalid = true;
	
	private Map<String, TreeItem> imNodeMap;
	private TreeMap<String, IEclipsePreferences[]> nodeMap;
	
	private static final int INIT_INX = 0;
	private static final int FORCE_INX = 1;
	
	private enum PrefValueType {NONE, ONE, ADDED, SAME, DIFFERENT};
	
	public CommonPrefPreferencePage() {
		super();
		setPreferenceStore(StartupPlugin.getDefault().getPreferenceStore());
		setDescription("The Common Preferences Manager handles loading of common preferences.");
		
	}

	@Override
	public boolean performOk() {
		acceptValues();
		
		return super.performOk();
	}

	@Override
	public boolean performCancel() {
		StartupPlugin.getDefault().clearCommonPrefFiles();
		
		return super.performCancel();
	}
	
	@Override
	protected void performDefaults() {
		boolean canRestore = MessageDialog.openConfirm(
				getShell(),
				"Remove all user entries?",
				"OK to remove all user defined entries in the list?");
		if (canRestore){
			CommonPrefEPFResources prefFiles = StartupPlugin.getDefault().getCommonPrefFiles(null);
			prefFiles.removeUserEntries();
			updatePrefFileTable(null);
		}
		
		super.performDefaults();
	}

	private boolean acceptValues() {
		
		CommonPrefEPFResources prefFiles = StartupPlugin.getDefault().getCommonPrefFiles(null);
		prefFiles.save();
		
		setEnableRead(prefLoadButton.getSelection());
		setShowCurrent(prefCompareCurrentButton.getSelection());
		
		return true;
	}
	
	@Override
	protected Control createContents(Composite parent) {
		prefFolder = new TabFolder(parent, SWT.NONE);
		prefFolder.setLayout(new TabFolderLayout());	
		prefFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		TabItem item = new TabItem(prefFolder, SWT.NONE);
		item.setText("General");
		item.setControl(createGeneralPage(prefFolder));
		
		item = new TabItem(prefFolder, SWT.NONE);
		item.setText("Compare Preferences");
		item.setControl(createPropertiesComparePage(prefFolder));
		
		prefFolder.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			public void widgetSelected(SelectionEvent e) {
				// If going to the second page update the file list if needed
				if (prefFolder.getSelectionIndex() == 1 && isComparePageInvalid) {
					initComparePage();
				}
			}
		});
		
		Dialog.applyDialogFont(prefFolder);
		return prefFolder;			
	}
	
	protected Control createGeneralPage(Composite parent) {

		// Create the main composite
		Composite prefPage = new Composite(parent, SWT.NONE);
		GridData gdpp = new GridData(SWT.LEFT, SWT.TOP, false, false);
		prefPage.setLayoutData(gdpp);
		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = 0;
		layout.numColumns = 1;
		prefPage.setLayout(layout);

		initializeDialogUnits(prefPage);
		
		// Add a label text describing the purpose of the Common Prefs		
		prefGenInfoText = new Label(prefPage, SWT.WRAP);
		prefGenInfoText.setText(getGenInfoText());
		GridData gdcpl = new GridData(SWT.LEFT, SWT.TOP, false, false);
		prefGenInfoText.setLayoutData(gdcpl);

		prefLoadButton = new Button(prefPage, SWT.CHECK);
		prefLoadButton.setText("Enable reading of preference files");
		prefLoadButton.setSelection(getEnbleRead());
		GridData gdlb = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gdlb.verticalIndent = 16;
		prefLoadButton.setLayoutData(gdlb);
		prefLoadButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				updateImportEnablement();
			}
		});
		
/*		
		// Add a text input for setting timeout for http input
		// TODO: Not completed - need add of column. and property
		Label prefTimeoutLbl = new Label(prefPage, 0);
		prefTimeoutLbl.setText("Timeout for getting http files");
		GridData ptlpl = new GridData(SWT.LEFT, SWT.TOP, false, false);
		prefTimeoutLbl.setLayoutData(ptlpl);
		
		Text prefTimeoutText = new Text(prefPage, SWT.SINGLE | SWT.LEFT);
		// prefTimeoutText.setText("Enable reading of preference files");
		GridData ptlb = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		ptlb.verticalIndent = 16;
		prefTimeoutText.setLayoutData(ptlb);
*/
		
		// ======================================
	    // Add a table for editing the pref files		

		// New composite for handling the controls
		Composite inner = new Composite(prefPage, SWT.NULL);
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layoutInner = new GridLayout(3, false);
		layoutInner.marginHeight = 0;
		layoutInner.marginWidth = 0;
		inner.setLayout(layoutInner);
		
		prefPathTable = new Table(inner, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER /*| SWT.CHECK*/);
		prefPathTable.setHeaderVisible(true);
		GridData gdpt = new GridData(SWT.FILL, SWT.FILL, true, true);
		gdpt.horizontalSpan = 2;
		gdpt.verticalSpan = 6;
		prefPathTable.setLayoutData(gdpt);
		prefPathTable.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				updateTableSize(prefPathTable);
			}
		});
		
			
		prefPathTable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
/*				
				Display d = getShell().getDisplay();
				Point pGlobal = d.getCursorLocation();
				Point p = d.map(null, prefPathTable, pGlobal);
				
				// TODO: Check which column the user clicked in and if in the Type
				// column, create an editor for that item. Find example in Eclipse.
				// Now using a separate combobox instead. Not as elegant, but working
*/				
				updateImportEnablement();
			}
		});

		new TableColumn(prefPathTable, SWT.NULL);
		new TableColumn(prefPathTable, SWT.NULL);
		TableColumn[] columns = prefPathTable.getColumns();
		
		columns[0].setResizable(true);
		columns[1].setResizable(true);		
		columns[0].setText("Preference files");
		columns[1].setText("Type");
		
		// ======================================
	    // Add buttons	
		
		prefFileUpButton = new Button(inner, SWT.PUSH);
		prefFileUpButton.setText("Move Up"); 
		GridData gdub = new GridData(SWT.LEFT, SWT.TOP, false, false);
		int btnWidthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		gdub.widthHint = Math.max(btnWidthHint, prefFileUpButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		prefFileUpButton.setLayoutData(gdub);
		prefFileUpButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TableItem[] selItems = prefPathTable.getSelection();
				if (selItems == null || selItems.length != 1)
					return;
				CommonPrefEPFResource pfMov = (CommonPrefEPFResource) selItems[0].getData();
				CommonPrefEPFResources prefFiles = StartupPlugin.getDefault().getCommonPrefFiles(null);
				if (prefFiles.movePrefFile(pfMov, false))
					updatePrefFileTable(pfMov);
			}
		});	
		
		prefFileDownButton = new Button(inner, SWT.PUSH);
		prefFileDownButton.setText("Move Down"); 
		GridData gddb = new GridData(SWT.LEFT, SWT.TOP, false, false);
		gddb.widthHint = Math.max(btnWidthHint, prefFileDownButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		prefFileDownButton.setLayoutData(gddb);
		prefFileDownButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TableItem[] selItems = prefPathTable.getSelection();
				if (selItems == null || selItems.length != 1)
					return;
				CommonPrefEPFResource pfMov = (CommonPrefEPFResource) selItems[0].getData();
				CommonPrefEPFResources prefFiles = StartupPlugin.getDefault().getCommonPrefFiles(null);
				if (prefFiles.movePrefFile(pfMov, true))
					updatePrefFileTable(pfMov);
			}
		});	
		
		prefFileAddButton = new Button(inner, SWT.PUSH);
		prefFileAddButton.setText("Add..."); 
		GridData gdab = new GridData(SWT.LEFT, SWT.TOP, false, false);
		gdab.widthHint = Math.max(btnWidthHint, prefFileAddButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		prefFileAddButton.setLayoutData(gdab);
		prefFileAddButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {

				// Get selection
				CommonPrefEPFResource selectedPrefFile = null;
				TableItem[] selItems = prefPathTable.getSelection();
				if (selItems != null && selItems.length == 1)
					selectedPrefFile = (CommonPrefEPFResource) selItems[0].getData();
				
				// Create and open dialog
				PrefFileDialog pfd = new PrefFileDialog(
						getShell(),
						PrefFileDialogType.PREF_FILE_ADD,
						selectedPrefFile);
				
				int res = pfd.open();
				if (res != Dialog.OK)
					return;
				CommonPrefEPFResource pf = pfd.getValue();
				updatePrefFileTable(pf);
			}
		});	
		
		prefFileEditButton = new Button(inner, SWT.PUSH);
		prefFileEditButton.setText("Edit..."); 
		GridData gdeb = new GridData(SWT.LEFT, SWT.TOP, false, false);
		gdeb.widthHint = Math.max(btnWidthHint, prefFileEditButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		prefFileEditButton.setLayoutData(gdeb);
		prefFileEditButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {

				// Get selection
				CommonPrefEPFResource selectedPrefFile = null;
				TableItem[] selItems = prefPathTable.getSelection();
				if (selItems != null && selItems.length == 1)
					selectedPrefFile = (CommonPrefEPFResource) selItems[0].getData();
				if (selectedPrefFile == null)
					return;
				
				// Create and open dialog
				PrefFileDialog pfd = new PrefFileDialog(
						getShell(),
						PrefFileDialogType.PREF_FILE_EDIT,
						selectedPrefFile);
				
				int res = pfd.open();
				if (res != Dialog.OK)
					return;
				CommonPrefEPFResource pf = pfd.getValue();
				updatePrefFileTable(pf);
			}
		});		
		
		prefFileRemoveButton = new Button(inner, SWT.PUSH);
		prefFileRemoveButton.setText("Remove"); 
		GridData gdrb = new GridData(SWT.LEFT, SWT.TOP, false, false);
		gdrb.widthHint = Math.max(btnWidthHint, prefFileRemoveButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		prefFileRemoveButton.setLayoutData(gdrb);
		prefFileRemoveButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {

				TableItem[] selItems = prefPathTable.getSelection();
				if (selItems == null || selItems.length == 0)
					return;
				
				boolean needUpdate = false;
				CommonPrefEPFResources prefFiles = StartupPlugin.getDefault().getCommonPrefFiles(null);
				for (TableItem item : selItems) {
					CommonPrefEPFResource pf = (CommonPrefEPFResource) item.getData();
					
					boolean okToRemove = true;
					if (okToRemove && prefFiles.removePrefFile(pf)) 
						needUpdate = true;
				}
				if (needUpdate)
					updatePrefFileTable(null);
			}
		});		
		
		prefFileLoadButton = new Button(inner, SWT.PUSH);
		prefFileLoadButton.setText("Load..."); 
		GridData gdflb = new GridData(SWT.LEFT, SWT.TOP, false, false);
		gdflb.verticalIndent = 16;
		gdflb.widthHint = Math.max(btnWidthHint, prefFileLoadButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		prefFileLoadButton.setLayoutData(gdflb);
		prefFileLoadButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {

				boolean ok = MessageDialog.openQuestion(
						getShell(),
						"Load selected preferences?",
						"This will load the preferences in the selected file.\n\n" + 
						"NOTE: To see changed preferences you might need to reopen\n" + 
						"the preferences dialog.\n\n" + 
						"OK to proceed?");
				if (!ok)
					return;
				
				TableItem[] selItems = prefPathTable.getSelection();
				if (selItems == null || selItems.length != 1)
					return;
				
				CommonPrefEPFResource pf = (CommonPrefEPFResource) selItems[0].getData();
				IStatus s = CommonPrefsHelper.loadPreferenceFile(pf, null);
				if (!(s.getSeverity() == IStatus.INFO || s.isOK())) {
					ErrorDialog.openError(
							getShell(),
							"Preference Load failed",
							"One or more errors ocurred when trying to import the selected preferences.",
							s);							
				}
			}
		});		
	
		// ======================================
	    // Add combo to set init/force value	
		
		Label createFileTypeLabel = new Label(inner, SWT.LEFT);
		createFileTypeLabel.setText("Preference File Type: ");
		GridData gdftl = new GridData(SWT.LEFT, SWT.TOP, false, true);
		gdftl.verticalIndent = 2;
		createFileTypeLabel.setLayoutData(gdftl);	
		
		prefFileTypeCombo = new Combo(inner, SWT.DROP_DOWN);
		GridData gdftc = new GridData(SWT.LEFT, SWT.TOP, false, true);
		prefFileTypeCombo.setLayoutData(gdftc);
		prefFileTypeCombo.add("init", INIT_INX);
		prefFileTypeCombo.add("force", FORCE_INX);
		prefFileTypeCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableItem[] selItems = prefPathTable.getSelection();
				if (selItems == null || selItems.length != 1)
					return;
				CommonPrefEPFResource pf = (CommonPrefEPFResource) selItems[0].getData();
				
				// Check if selection is changed
				boolean isForce = (prefFileTypeCombo.getSelectionIndex() == FORCE_INX);
				if (pf.isForce() == isForce)
					return;
				
				// Update the value of the file
				pf.setIsForce(isForce);
				updatePrefFileTable(pf);
			}
		});
				
		// Fill in the table
		updatePrefFileTable(null);
		
		applyDialogFont(prefPage);
		
		return prefPage;
	}

	protected Control createPropertiesComparePage(Composite parent) {

		// Create the main composite
		Composite prefPage = new Composite(parent, SWT.NONE);
		GridData gdpp = new GridData(SWT.LEFT, SWT.TOP, false, false);
		prefPage.setLayoutData(gdpp);
		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = 0;
		layout.numColumns = 1;
		prefPage.setLayout(layout);
		
		initializeDialogUnits(prefPage);

		// Add a label text describing the Compare page		
		prefCompInfoText = new Label(prefPage, SWT.WRAP);
		prefCompInfoText.setText(getCompInfoText());
		GridData gdcpl = new GridData(SWT.LEFT, SWT.TOP, false, false);
		prefCompInfoText.setLayoutData(gdcpl);	
		
		prefCompareCurrentButton = new Button(prefPage, SWT.CHECK);
		prefCompareCurrentButton.setText("Show current value for each preference key");
		prefCompareCurrentButton.setSelection(getShowCurrent());
		GridData gdlb = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gdlb.verticalIndent = 16;
		prefCompareCurrentButton.setLayoutData(gdlb);
		prefCompareCurrentButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				updateCompareTree();
			}
		});		
		
		// Create a checkboxed list of preference files
		prefComparePathTable = new Table(prefPage, SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK);
		prefComparePathTable.setHeaderVisible(true);
		GridData gdpt = new GridData(SWT.FILL, SWT.TOP, true, false);
		prefComparePathTable.setLayoutData(gdpt);		
		prefComparePathTable.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				updateTableSize(prefComparePathTable);
			}
		});
		
		prefComparePathTable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Since tree needs to be completely recreated for any change
				// no need to check what item that caused the selection event
				updateCompareTree();
			}
		});

		new TableColumn(prefComparePathTable, SWT.NULL);
		new TableColumn(prefComparePathTable, SWT.NULL);
		TableColumn[] columns = prefComparePathTable.getColumns();
		
		columns[0].setResizable(true);
		columns[1].setResizable(true);		
		columns[0].setText("Preference files");
		columns[1].setText("Type");		
		
		// Create a tree showing the preferences for the selected files
		imPrefTree = new Tree(prefPage, SWT.MULTI | SWT.BORDER);
		GridData gdibst = new GridData(SWT.FILL, SWT.FILL, true, true);
		imPrefTree.setLayoutData(gdibst);
		imPrefTree.setHeaderVisible(true);
		
		imPrefTree.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				updateTreeSize();
			}
		});
		
		TreeColumn tc = new TreeColumn(imPrefTree, SWT.LEFT);
		tc.setText("Preference");
		String toolTip = "The preference key. A green icon indicates that all values are\n";
		toolTip += "same, a red icon indicates that one or more values differ and a blue\n";
		toolTip += "icon indicates that the value is an added value (i.e. has no default)";
		tc.setToolTipText(toolTip);
		tc.setResizable(true);
				
		initComparePage();
		
		return prefPage;
	}
	
	private void clearCompareTree() {
		
		// Clear any old items
		imPrefTree.removeAll();
		
		// Remove all columns but the first which is showing keys
		TreeColumn[] cols = imPrefTree.getColumns();
		for (int i = 1; i < cols.length; i++) {
			TreeColumn treeColumn = cols[i];
			treeColumn.dispose();
		}		
	}
	
	/**
	 * Loop through the list of pref files and for the selected files, add
	 * entries to the list in alphabetical order. The value for an entry
	 * should be added to correct column.
	 */
	private void updateCompareTree() {
		
		clearCompareTree();
	
		// If so selected, add column to show the current preferences value for
		// all preferences shown in the tree - or <none> if not present
		boolean showCurrent = prefCompareCurrentButton.getSelection();
		IPreferencesService prefsService = null;
		if (showCurrent) {
			TreeColumn tc = new TreeColumn(imPrefTree, SWT.LEFT);
			tc.setText("Current");
			tc.setResizable(true);
			tc.setToolTipText("The preference value currently used in Eclipse");
			
			// ADD THIS CODE FOR TESTING
			StartupPlugin.getDefault().getNetworkPrefResources().loadNetworkSettings();
			// END
			
			prefsService = Platform.getPreferencesService();
		}		
		
		// Add a column per selected file
		TableItem[] items = prefComparePathTable.getItems();
		boolean nodeMask[] = new boolean[items.length];
		for (int i = 0; i < items.length; i++) {
			TableItem tableItem = items[i];
			nodeMask[i] = tableItem.getChecked();
			if (!nodeMask[i])
				continue;
			
			TreeColumn tc = new TreeColumn(imPrefTree, SWT.LEFT);
			String fileNoStr = (new Integer(i + 1)).toString();
			tc.setText("File " + fileNoStr);
			tc.setToolTipText("The preference value from File " + fileNoStr);			
			tc.setResizable(true);
		}
		
		// Add the nodes to the tree
		imNodeMap = new HashMap<String, TreeItem>();
		Collection<IEclipsePreferences[]> nodesColl = nodeMap.values();
		for (IEclipsePreferences[] nodes : nodesColl) {    	
	    	addNode(nodes, nodeMask, prefsService);
	    }
		
		updateRootNodeImages();

		updateTreeSize();
	}

	private void addNode(
			IEclipsePreferences[] nodes,
			boolean nodeMask[],
			IPreferencesService prefsService) {	
		
		TreeMap<String, String[]> keyValueMap = new TreeMap<String, String[]>();
		
		
		// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
		// Retrieve the list of eclipse network connection settings
		//NetworkPrefResources networkPrefResources = StartupPlugin.getDefault().getNetworkPrefResources();
		//Map<String, NetworkPrefResource> networkSettingsMap = networkPrefResources.getNetworkPreferences();
		// END
		
		// The nodes array contains at least one valid node. Add this to the tree,
		// and collect the values from this and any other nodes to a map so they
		// are sorted and filtered.
		TreeItem pNode = null;
		int noOfNodes = nodes.length;
		String nodeName = "";
		for (int i = 0; i < noOfNodes; i++) {
			if (!nodeMask[i])
				continue; // Pref file not selected
			
			IEclipsePreferences node = nodes[i];
			
			if (node == null)
				continue; // Node not present - OK
			
			// Get node name for later use
			nodeName = node.absolutePath();
						
			// Get parent node from map. If parents hasn't yet been added
			// add parents recursively upwards in tree. 
			if (pNode == null) {
				pNode = CommonPrefsHelper.addNodeInternal(node, imNodeMap, imPrefTree);
					
			}
			
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
				continue;
			
			// Add keys array to TreeSet to order (and filter) them
			for (String key : keys) {
				String[] values = keyValueMap.get(key);
				if (values == null) {
					values = new String[noOfNodes];
					keyValueMap.put(key, values);
				}
				values[i] = node.get(key, "<default>");
				
			}			
		}	// End the upper for loop
		
		if (pNode == null)
			return; // Error
		
		ImageRegistry reg = StartupPlugin.getDefault().getImageRegistry();
		
		// Variable describing the current value(s) for the node
		PrefValueType vType = PrefValueType.NONE;
		
		// ADD THIS CODE FOR TESTING
		IScopeContext[] scopeContext = {new InstanceScope()};
		// END
		
		// Add key nodes and for each node, available values to the tree
		Set<String> keys = keyValueMap.keySet();
		for (String key : keys) {
			TreeItem pdItem = new TreeItem(pNode, SWT.NONE);
			pdItem.setText(key);
			
			//System.err.println("Key: " + key);
			// End
			
			String prevValue = null;
			
			// If present and selected, add the current workspace preference
			// value for the selected node. Or <none> if not present.
			int column = 0;
			if (prefsService != null) {
				column++;
				
				//ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
				IEclipsePreferences rootNode = prefsService.getRootNode();
				IEclipsePreferences node = (IEclipsePreferences) rootNode.node(InstanceScope.SCOPE).node(nodeName);
				String val = node.get(key, "<none>");				
				// END
								
				//String val = prefsService.getString(nodeName, key, "<none>", scopeContext);
										
				//System.err.println("If present and selected, add the current workspace preference - column: " + column + ",   value: " + val);
				pdItem.setText(column, val);
				
				prevValue = (val.compareTo("<none>") == 0)? null : val;
				
				// If no current value defined, this key is new 
				vType = (prevValue == null)? PrefValueType.ADDED : PrefValueType.NONE;
			}	// End if
			
			// Add values for keys to the correct columns
			String[] values = keyValueMap.get(key);
			for (int i = 0; i < noOfNodes; i++) {
				if (!nodeMask[i])
					continue; // Pref file not selected
				
				column++;
								
				String value = values[i];
				if (value == null)
					continue; // No value defined for key
				
				//System.err.println("Add values for keys to the correct columns - column: " + column + ",   value: " + value);
				pdItem.setText(column, value);
				
				// Check if we have more than one value, and if so
				// if all are same or that at least one is different
				if (prevValue == null)
					prevValue = value;
				else {
					if (prevValue.compareTo(value) != 0)
						vType = PrefValueType.DIFFERENT;
					else if (vType != PrefValueType.DIFFERENT)
						vType = PrefValueType.SAME;
				}				
			}	// End the lower for loop
			
			// Add an icon showing compare state if at least two values for the key
			markRootNodeImage(pNode, vType);
			if (vType == PrefValueType.DIFFERENT) 
				pdItem.setImage(reg.get(StartupPlugin.EXP_TREE_DIFF_VAL_IMG));
			else if (vType == PrefValueType.SAME)
				pdItem.setImage(reg.get(StartupPlugin.EXP_TREE_SAME_VAL_IMG));
			else if (vType == PrefValueType.ADDED)
				pdItem.setImage(reg.get(StartupPlugin.EXP_TREE_ADDED_VAL_IMG));
		}	// End outer for
	}
	
	
	/**
	 * ADD THIS METHOD FOR TESTING
	 * 
	 */
	private void retrieveNode(TreeMap<String, String[]> keyValueMap, TreeItem pNode, IPreferencesService prefsService,
						String nodeName, PrefValueType vType, int noOfNodes, boolean[] nodeMask, IEclipsePreferences[] nodes,
						ImageRegistry reg) {
		
		// Add key nodes and for each node, available values to the tree
		Set<String> keys = keyValueMap.keySet();
		for (String key : keys) {
			TreeItem pdItem = new TreeItem(pNode, SWT.NONE);
			pdItem.setText(key);
			
			// Added this code for testing
			//System.err.print("Key: " + key);
			// End
			
			String prevValue = null;
			
			// If present and selected, add the current workspace preference
			// value for the selected node. Or <none> if not present.
			int column = 0;
			if (prefsService != null) {
				column++;
				String val = prefsService.getString(nodeName, key, "<none>", null);
				
				// ADD THIS CODE FOR TESTING
				/*String value = prefsService.get(key, "<none>", nodes);
				System.out.print("NODE NAME: " + nodeName );
				System.err.println("  -   KEY: " + key + " -  Value: " + value);*/
				// END
				
				pdItem.setText(column, val);
				prevValue = (val.compareTo("<none>") == 0)? null : val;
				
				// If no current value defined, this key is new 
				vType = (prevValue == null)? 
						PrefValueType.ADDED:
						PrefValueType.NONE;
			}	// End if
			
			// Add values for keys to the correct columns
			String[] values = keyValueMap.get(key);
			for (int i = 0; i < noOfNodes; i++) {
				if (!nodeMask[i])
					continue; // Pref file not selected
				
				column++;
				
				String value = values[i];
				if (value == null)
					continue; // No value defined for key
				
				pdItem.setText(column, value);
				
				//System.err.println("Column: " + column + "     Value: " + value);
				
				// Check if we have more than one value, and if so
				// if all are same or that at least one is different
				if (prevValue == null)
					prevValue = value;
				else {
					if (prevValue.compareTo(value) != 0)
						vType = PrefValueType.DIFFERENT;
					else if (vType != PrefValueType.DIFFERENT)
						vType = PrefValueType.SAME;
				}				
			}	// End the inner for
			
			// Add an icon showing compare state if at least two values for the key
			markRootNodeImage(pNode, vType);
			if (vType == PrefValueType.DIFFERENT) 
				pdItem.setImage(reg.get(StartupPlugin.EXP_TREE_DIFF_VAL_IMG));
			else if (vType == PrefValueType.SAME)
				pdItem.setImage(reg.get(StartupPlugin.EXP_TREE_SAME_VAL_IMG));
			else if (vType == PrefValueType.ADDED)
				pdItem.setImage(reg.get(StartupPlugin.EXP_TREE_ADDED_VAL_IMG));
		}	// End outer for
	}
	
	

	/**
	 * Mark a plugin node with correct image key. After all nodes and keys are
	 * added, this will be used to set the images for the plugin nodes. The
	 * precedence order of the keys are DIFF, SAME, ADDED, i.e. if one subnode
	 * is DIFF, the plugin node is also marked as DIFF etc.
	 * 
	 * @param item
	 * @param diffValues
	 */
	private void markRootNodeImage(TreeItem item, PrefValueType vType) {
		
		TreeItem pluginNode = CommonPrefsHelper.getPluginNode(item);

		String currKey = (String) pluginNode.getData("image");
		switch (vType) {
		case DIFFERENT:
			pluginNode.setData("image", StartupPlugin.EXP_TREE_PLUGIN_DIFF_IMG);
			break;
		case SAME:
			if (currKey != StartupPlugin.EXP_TREE_PLUGIN_DIFF_IMG)
				pluginNode.setData("image", StartupPlugin.EXP_TREE_PLUGIN_SAME_IMG);
			break;
		case ADDED:
			if (currKey != StartupPlugin.EXP_TREE_PLUGIN_DIFF_IMG &&
					currKey != StartupPlugin.EXP_TREE_PLUGIN_SAME_IMG)
				pluginNode.setData("image", StartupPlugin.EXP_TREE_PLUGIN_ADDED_IMG);			
			break;			
		default:
			break;
		}	
	}	

	/**
	 * Update the root node images based based on the tag associated.
	 * The default images are already set, so update only if new image
	 * is specified.
	 */
	private void updateRootNodeImages(){
		
		ImageRegistry reg = StartupPlugin.getDefault().getImageRegistry();
		List<TreeItem> items = CommonPrefsHelper.getPluginNodes(imPrefTree);
		for (TreeItem treeItem : items) {
			String key = (String) treeItem.getData("image");
			if (key != null)
				treeItem.setImage(reg.get(key));
		}
	}
	
	private boolean getEnbleRead() {
		
		IPreferenceStore store = StartupPlugin.getDefault().getPreferenceStore();
		boolean readPrefs = true;
		if (store.contains(PreferenceInitializer.PREF_ENABLE_READ))
			readPrefs = store.getBoolean(PreferenceInitializer.PREF_ENABLE_READ);
		return readPrefs;
	}
	
	private void setEnableRead(boolean enableRead) {
		
		if (enableRead == getEnbleRead())
			return;
		
		IPreferenceStore store = StartupPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceInitializer.PREF_ENABLE_READ, enableRead);
	}
	
	private boolean getShowCurrent() {
		IPreferenceStore store = StartupPlugin.getDefault().getPreferenceStore();
		boolean showCurrent = true;
		if (store.contains(PreferenceInitializer.PREF_SHOW_CURRENT))
			showCurrent = store.getBoolean(PreferenceInitializer.PREF_SHOW_CURRENT);
		return showCurrent;
	}
	
	private void setShowCurrent(boolean showCurrent) {
		if (showCurrent == getShowCurrent())
			return;
		
		IPreferenceStore store = StartupPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceInitializer.PREF_SHOW_CURRENT, showCurrent);
	}	
	
	private String getGenInfoText() {
		
		// NOTE: Platform dependent behavior where on solaris, the lines are wrapped to a resonable 
		// default size, but on windows the text without breaks will make the dialog too wide. 
		String lineBrk = "";
		if (Platform.getOS().compareTo(Platform.OS_WIN32) == 0)
			lineBrk = "\n";
		
		String msg = "The Common Preferences Manager will read preference files on startup of Eclipse. " + lineBrk;
		msg += "Entries with gray text are either provided in the config.ini file or in a .ref file." + lineBrk;
		msg += "These can not be edited here.\n\n";
		msg += "You can add local entries that will override the common settings in the dialog below. " + lineBrk;
		msg += "A file with Type 'init' will be read only once for each workspace. A file with Type 'force' " + lineBrk;
		msg += "will be read each time Eclipse is started.";

		return msg;
	}

	private String getCompInfoText() {
		
		// NOTE: Platform dependent behavior where on solaris, the lines are wrapped to a resonable 
		// default size, but on windows the text without breaks will make the dialog too wide. 
		String lineBrk = "";
		if (Platform.getOS().compareTo(Platform.OS_WIN32) == 0)
			lineBrk = "\n";
		
		String msg = "Select one or more of the preference files below to compare their values." + lineBrk;
		msg += "To also compare with the currently used value, select the checkbox below." + lineBrk;

		return msg;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		// Clear any cached info to avoid having stale fileinfo
		StartupPlugin.getDefault().clearCommonPrefFiles();
	}
	
	/**
	 * Update the list of preference files that are set by the user for the Compare
	 * Tab. Add a number showing relative position in the list
	 */
	private void initComparePage() {
		// Remove old entries from the table
		prefComparePathTable.removeAll();
		
		updateTableSize(prefComparePathTable);

		ImageRegistry reg = StartupPlugin.getDefault().getImageRegistry();		
		
		// Add all pref files to the list and add all nodes to a sorted map
		nodeMap = new TreeMap<String, IEclipsePreferences[]>();
		CommonPrefEPFResources prefFiles = StartupPlugin.getDefault().getCommonPrefFiles(null);
		int fileNo = 0;
		int noOfFiles = prefFiles.length();
		for (Iterator<CommonPrefEPFResource> iter = prefFiles.iterator(); iter.hasNext();) {
			CommonPrefEPFResource pf = (CommonPrefEPFResource) iter.next();
			TableItem item = new TableItem(prefComparePathTable, SWT.DEFAULT);
			item.setText(0, (new Integer(fileNo + 1)).toString() + ") " + pf.getResourceName());
			item.setText(1, (pf.isForce()? "force" : "init"));

			if (pf.exists())
				item.setImage(reg.get(StartupPlugin.PREF_FILE_EXIST_IMG));
			else
				item.setImage(reg.get(StartupPlugin.PREF_FILE_NOEXIST_IMG));
			
			IExportedPreferences prefs = CommonPrefsHelper.readPreferences(pf);
			if (prefs != null)
				CommonPrefsHelper.sortPreferences(prefs, fileNo, noOfFiles, nodeMap);
			
			fileNo++;
		}
		
		this.
		
		// Update the compare tree
		updateCompareTree();
		
		isComparePageInvalid = false;
	}
	
	private void updatePrefFileTable(CommonPrefEPFResource selectPrefFile) {		
		prefPathTable.removeAll();
		prefFileTypeCombo.setText("");
		
		updateTableSize(prefPathTable);
		
		Color grayTextColor = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
		ImageRegistry reg = StartupPlugin.getDefault().getImageRegistry();
		
		CommonPrefEPFResources prefFiles = StartupPlugin.getDefault().getCommonPrefFiles(null);
		for (Iterator<CommonPrefEPFResource> iter = prefFiles.iterator(); iter.hasNext();) {
			CommonPrefEPFResource pf = (CommonPrefEPFResource) iter.next();
			TableItem item = new TableItem(prefPathTable, SWT.DEFAULT);
			item.setText(0, pf.getResourceName());
			item.setText(1, (pf.isForce()? "force" : "init"));
			
			if (pf.exists())
				item.setImage(reg.get(StartupPlugin.PREF_FILE_EXIST_IMG));
			else
				item.setImage(reg.get(StartupPlugin.PREF_FILE_NOEXIST_IMG));
			
			// Mark element as "admin" by setting the text gray (disabled)
			// if it's not a user entry. This based on if its located in a
			// config file (.ini or .ref)
			if (pf.isConfig())
				item.setForeground(grayTextColor);
			
			item.setData(pf);
			
			if (selectPrefFile != null && selectPrefFile.equals(pf))
				prefPathTable.setSelection(item);
		}
		
		// Update page enablement
		updateImportEnablement();
		
		// Mark the Compare page invalid since list changed
		isComparePageInvalid = true;
	}
	
	private void updateImportEnablement() {
		boolean isEnabled = prefLoadButton.getSelection();
		
		prefPathTable.setEnabled(isEnabled);
		prefFileAddButton.setEnabled(isEnabled);
		
		boolean isConfig = isEnabled;
		boolean isSelOne = isEnabled;
		boolean canMoveUp = false;
		boolean canMoveDown = false;
		if (isSelOne) {
			TableItem[] selItems = prefPathTable.getSelection();
			isSelOne = (selItems != null && selItems.length == 1);
			if (isSelOne) {
				CommonPrefEPFResource pf = (CommonPrefEPFResource) selItems[0].getData();
				prefFileTypeCombo.select(pf.isForce()? FORCE_INX : INIT_INX);
				isConfig = pf.isConfig();
				if (!isConfig) {
					CommonPrefEPFResources prefFiles = StartupPlugin.getDefault().getCommonPrefFiles(null);
					canMoveUp = prefFiles.canMovePrefFile(pf, false);
					canMoveDown = prefFiles.canMovePrefFile(pf, true);
				}
			}
		}
		
		prefFileUpButton.setEnabled(canMoveUp);
		prefFileDownButton.setEnabled(canMoveDown);
		prefFileEditButton.setEnabled(isSelOne && !isConfig);
		
		// TODO: Possible OK to also have enabled if > 1 selected
		prefFileRemoveButton.setEnabled(isSelOne && !isConfig);
		prefFileTypeCombo.setEnabled(isSelOne && !isConfig);
		prefFileLoadButton.setEnabled(isSelOne);
	}
	
	/**
	 * Compute size of table relative its bounds.
	 * TODO: Handle also resize within table.
	 */
	private void updateTableSize(Table t) {
		TableColumn[] columns = t.getColumns();
		int w = t.getSize().x;
		int width = (w * 84) / 100;
		columns[0].setWidth(width);
		
		// NOTE: Make sum width - 4 since avoids the scroll bar
		columns[1].setWidth(w - width - 4);	
	}
	
	/**
	 * Compute size of table relative its bounds.
	 * TODO: Handle also resize within table.
	 */
	private void updateTreeSize() {
		TreeColumn[] columns = imPrefTree.getColumns();
		int w = imPrefTree.getSize().x;
		int colX = 60 / (columns.length > 1? (columns.length - 1) : 1);
		int width = (w * colX) / 100;
		
		// NOTE: Make sum width - 4 since avoids the scroll bar
		columns[0].setWidth(w - 4 - (width * (columns.length - 1)));
		
		for (int i = 1; i < columns.length; i++) {
			columns[i].setWidth(width);
		}
	}		
}