package org.cryptomator.ui.traymenu;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.fxapp.FxApplicationTerminator;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.ResourceBundle;
import java.util.function.Consumer;

@TrayMenuScoped
class TrayMenuController {

	private final ResourceBundle resourceBundle;
	private final VaultService vaultService;
	private final FxApplicationWindows appWindows;
	private final FxApplicationTerminator appTerminator;
	private final ObservableList<Vault> vaults;
	private final PopupMenu menu;

	@Inject
	TrayMenuController(ResourceBundle resourceBundle, VaultService vaultService, FxApplicationWindows appWindows, FxApplicationTerminator appTerminator, ObservableList<Vault> vaults) {
		this.resourceBundle = resourceBundle;
		this.vaultService = vaultService;
		this.appWindows = appWindows;
		this.appTerminator = appTerminator;
		this.vaults = vaults;
		this.menu = new PopupMenu();
	}

	public PopupMenu getMenu() {
		return menu;
	}

	public void initTrayMenu() {
		vaults.addListener(this::vaultListChanged);
		vaults.forEach(v -> {
			v.displayNameProperty().addListener(this::vaultListChanged);
		});
		rebuildMenu();
	}

	private void vaultListChanged(@SuppressWarnings("unused") Observable observable) {
		assert Platform.isFxApplicationThread();
		rebuildMenu();
	}

	private void rebuildMenu() {
		menu.removeAll();

		MenuItem showMainWindowItem = new MenuItem(resourceBundle.getString("traymenu.showMainWindow"));
		showMainWindowItem.addActionListener(this::showMainWindow);
		menu.add(showMainWindowItem);

		MenuItem showPreferencesItem = new MenuItem(resourceBundle.getString("traymenu.showPreferencesWindow"));
		showPreferencesItem.addActionListener(this::showPreferencesWindow);
		menu.add(showPreferencesItem);

		menu.addSeparator();
		for (Vault v : vaults) {
			MenuItem submenu = buildSubmenu(v);
			menu.add(submenu);
		}
		menu.addSeparator();

		MenuItem lockAllItem = new MenuItem(resourceBundle.getString("traymenu.lockAllVaults"));
		lockAllItem.addActionListener(this::lockAllVaults);
		lockAllItem.setEnabled(!vaults.filtered(Vault::isUnlocked).isEmpty());
		menu.add(lockAllItem);

		MenuItem quitApplicationItem = new MenuItem(resourceBundle.getString("traymenu.quitApplication"));
		quitApplicationItem.addActionListener(this::quitApplication);
		menu.add(quitApplicationItem);
	}

	private Menu buildSubmenu(Vault vault) {
		Menu submenu = new Menu(vault.getDisplayName());

		if (vault.isLocked()) {
			MenuItem unlockItem = new MenuItem(resourceBundle.getString("traymenu.vault.unlock"));
			unlockItem.addActionListener(createActionListenerForVault(vault, this::unlockVault));
			submenu.add(unlockItem);
		} else if (vault.isUnlocked()) {
			submenu.setLabel("* ".concat(submenu.getLabel()));

			MenuItem lockItem = new MenuItem(resourceBundle.getString("traymenu.vault.lock"));
			lockItem.addActionListener(createActionListenerForVault(vault, this::lockVault));
			submenu.add(lockItem);

			MenuItem revealItem = new MenuItem(resourceBundle.getString("traymenu.vault.reveal"));
			revealItem.addActionListener(createActionListenerForVault(vault, this::revealVault));
			submenu.add(revealItem);
		}

		return submenu;
	}

	private ActionListener createActionListenerForVault(Vault vault, Consumer<Vault> consumer) {
		return actionEvent -> consumer.accept(vault);
	}

	private void quitApplication(EventObject actionEvent) {
		appTerminator.terminate();
	}

	private void unlockVault(Vault vault) {
		appWindows.startUnlockWorkflow(vault, null);
	}

	private void lockVault(Vault vault) {
		appWindows.startLockWorkflow(vault, null);
	}

	private void lockAllVaults(ActionEvent actionEvent) {
		vaultService.lockAll(vaults.filtered(Vault::isUnlocked), false);
	}

	private void revealVault(Vault vault) {
		vaultService.reveal(vault);
	}

	void showMainWindow(@SuppressWarnings("unused") ActionEvent actionEvent) {
		appWindows.showMainWindow();
	}

	private void showPreferencesWindow(@SuppressWarnings("unused") EventObject actionEvent) {
		appWindows.showPreferencesWindow(SelectedPreferencesTab.ANY);
	}

}
