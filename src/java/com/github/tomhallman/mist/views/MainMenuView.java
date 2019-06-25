/**
 * MIST: eMail Import System for TntConnect
 * Copyright (C) 2010 Tom Hallman
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For more information, visit https://github.com/tomhallman/mist
 */

package com.github.tomhallman.mist.views;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import com.github.tomhallman.mist.util.ui.ImageManager;

public class MainMenuView {
    private static Logger log = LogManager.getLogger();

    private Shell shell = null;

    private MenuItem aboutItem = null;
    private MenuItem editSettingsItem = null;
    private MenuItem exitItem = null;
    private MenuItem manualItem = null;
    private Menu menu = null;

    public MainMenuView(Shell shell) {
        log.trace("MainMenuView({})", shell);
        this.shell = shell;

        /*
         * On Mac, the MIST Application menu will contain menu items for About, Preferences & Quit.
         * On Windows, we'll add these to other menus.
         */

        menu = new Menu(shell, SWT.BAR);

        if (!Util.isMac()) {
            MenuItem fileItem = new MenuItem(menu, SWT.CASCADE);
            fileItem.setText("&File");
            Menu fileMenu = new Menu(menu);
            fileItem.setMenu(fileMenu);
            exitItem = new MenuItem(fileMenu, SWT.NONE);
            exitItem.setText("E&xit");
            exitItem.setImage(ImageManager.getImage("exit"));
        }

        if (!Util.isMac()) {
            MenuItem settingsItem = new MenuItem(menu, SWT.CASCADE);
            settingsItem.setText("&Settings");
            Menu settingsMenu = new Menu(menu);
            settingsItem.setMenu(settingsMenu);
            editSettingsItem = new MenuItem(settingsMenu, SWT.NONE);
            editSettingsItem.setText("&Edit Settings...");
            editSettingsItem.setImage(ImageManager.getImage("settings"));
        }

        MenuItem helpItem = new MenuItem(menu, SWT.CASCADE);
        helpItem.setText("&Help");
        Menu helpMenu = new Menu(menu);
        helpItem.setMenu(helpMenu);
        manualItem = new MenuItem(helpMenu, SWT.NONE);
        manualItem.setText("&View Online User Manual");
        manualItem.setAccelerator(SWT.F1);
        manualItem.setImage(ImageManager.getImage("manual"));

        if (!Util.isMac()) {
            aboutItem = new MenuItem(helpMenu, SWT.NONE);
            aboutItem.setText("&About MIST...");
            aboutItem.setImage(ImageManager.getImage("about"));
        }

        shell.setMenuBar(menu);
    }

    public MenuItem getAboutItem() {
        return aboutItem;
    }

    public MenuItem getEditSettingsItem() {
        return editSettingsItem;
    }

    public MenuItem getExitItem() {
        return exitItem;
    }

    public MenuItem getManualItem() {
        return manualItem;
    }

    public Menu getMenu() {
        return menu;
    }

    public Shell getShell() {
        return shell;
    }

}
