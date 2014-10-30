/*
 * CTop
 *
 * Copyright (c) 2014, Savoir Technologies, Inc., All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package com.savoirtech.karaf.commands;

import java.io.IOException;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(scope = "aetos", name = "ctop", description = "Camel Karaf Top Command")
public class CTop extends AbstractAction {

    private int             DEFAULT_REFRESH_INTERVAL = 1000;
    private long            lastUpTime               = 0;

    @Argument(index = 0, name = "name", description = "The name of the Camel context", required = true, multiValued = false)
    private String name;

    @Option(name = "-u", aliases = { "--updates" }, description = "Update interval in milliseconds", required = false, multiValued = false)
    private String updates;


    protected Object doExecute() throws Exception {
        if (updates != null) {
             DEFAULT_REFRESH_INTERVAL = Integer.parseInt(updates);
        } 
        try {
            CTop();
        } catch (IOException e) {
            //Ignore
        }
        return null;
    }

    private void CTop() throws InterruptedException, IOException {

        // Continously update stats to console.
        while (true) {
            Thread.sleep(DEFAULT_REFRESH_INTERVAL);
            //Clear console, then print JVM stats
            clearScreen();
            System.out.printf(" ctop Camel Context:  Version:  Status:  Uptime: ");
            System.out.println();
            System.out.println("\u001B[36m==========================================================================================\u001B[0m");
            System.out.printf("    \u001B[1mRouteID Exch\u001B[0m Total Complete Failed \u001B[1mProcessing Time\u001B[0m Min Max Mean Total Last%n");
            System.out.println();
            // Display notifications
            System.out.printf(" Note: Context stats updated at  %d ms intervals", DEFAULT_REFRESH_INTERVAL);
            System.out.println();
            System.out.println("\u001B[36m==========================================================================================\u001B[0m");
        }
    }

    private void clearScreen() {
        System.out.print("\33[2J");
        System.out.flush();
        System.out.print("\33[1;1H");
        System.out.flush();
    }

}
