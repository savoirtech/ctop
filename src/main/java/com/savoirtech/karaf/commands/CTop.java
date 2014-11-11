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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.karaf.commands.CamelController;
import org.apache.camel.spi.ManagementAgent;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(scope = "aetos", name = "ctop", description = "Camel Karaf Top Command")
public class CTop extends AbstractAction {

    private int             DEFAULT_REFRESH_INTERVAL = 1000;
    private String[] values = new String[] {"ExchangesTotal", "ExchangesCompleted", "ExchangesFailed",
                                            "MinProcessingTime", "MaxProcessingTime", "MeanProcessingTime",
                                            "TotalProcessingTime", "LastProcessingTime"} ;
    private Set columns = new HashSet<String>(Arrays.asList(values));
    protected CamelController camelController;

    @Argument(index = 0, name = "name", description = "The name of the Camel context", required = true, multiValued = false)
    private String name;

    @Option(name = "-u", aliases = { "--updates" }, description = "Update interval in milliseconds", required = false, multiValued = false)
    private String updates;

    @Option(name = "-s", aliases = { "--sortBy" }, description = "Sort routes by Column Name", required = false, multiValued = false)
    private String column;

    @Option(name = "-o", aliases = { "--reverseOrder" }, description = "Reverse ordering", required = false, multiValued = false)
    private boolean ordering;


    protected Object doExecute() throws Exception {
        if (updates != null) {
            DEFAULT_REFRESH_INTERVAL = Integer.parseInt(updates);
        } 
        if (column == null) {
            column = "ExchangesTotal";
        }
        if (column != null) {
           if (!isColumnName(column)) {
               System.err.println("Column " + column + " does not exist.");
               System.err.println("Valid columns include: " + columns.toString());
               return null;
           }
        }
        CamelContext camelContext = camelController.getCamelContext(name);
        if (camelContext == null) {
            System.err.println("Camel context " + name + " not found.");
            return null; 
        }
        try {
            CTop(camelContext);
        } catch (IOException e) {
            //Ignore
        }
        return null;
    }

    private void CTop(CamelContext camelContext) throws InterruptedException, IOException, Exception {

        // Continously update stats to console.
        while (true) {
            Thread.sleep(DEFAULT_REFRESH_INTERVAL);
            //Clear console, then print JVM stats
            clearScreen();
            System.out.printf(" \u001B[1mctop\u001B[0m - Context: %s Version: %S Status: %S Uptime: %S%n", 
                                camelContext.getName(), 
                                camelContext.getVersion(),
                                camelContext.getStatus(),
                                camelContext.getUptime());
            System.out.printf(" Auto Startup: \u001B[36m%S\u001B[0m Starting Routes: \u001B[36m%S\u001B[0m Suspended: \u001B[36m%S\u001B[0m Tracing: \u001B[36m%S\u001B[0m%n", 
                                camelContext.isAutoStartup(), 
                                camelContext.isStartingRoutes(),
                                camelContext.isSuspended(),
                                camelContext.isTracing());
            System.out.printf(" Sorting On: \u001B[36m%s\u001B[0m Reverse Ordering: \u001B[36m%s\u001B[0m %n", column, ordering);
            System.out.println("\u001B[36m==========================================================================================\u001B[0m");
            System.out.printf("         \u001B[1mRouteID             Exchanges\u001B[0m                   \u001B[1mProcessing Time (ms)\u001B[0m%n");
            System.out.printf("                    Total Complete   Failed      Min      Max     Mean    Total     Last%n");
            System.out.println();
            printRouteStats(camelContext);
            // Display notifications
            System.out.printf(" Note: Context stats updated at  %d ms intervals. Use Control + c to exit.", DEFAULT_REFRESH_INTERVAL);
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

    private void printRouteStats(CamelContext camelContext) throws Exception {

        Map<String, Long> routeMap  = new TreeMap<String, Long>(); 
        Map<String, RouteInfo> routes = new HashMap<String, RouteInfo>();

        for (Route route : camelContext.getRoutes()) {
            ManagementAgent agent = camelContext.getManagementStrategy().getManagementAgent();
            if (agent != null) {

                MBeanServer mBeanServer = agent.getMBeanServer();
                Set<ObjectName> set = mBeanServer.queryNames(new ObjectName(agent.getMBeanObjectDomainName() + ":type=routes,name=\"" + route.getId() + "\",*"), null);
                Iterator<ObjectName> iterator = set.iterator();

                if (iterator.hasNext()) {
                     ObjectName routeMBean = iterator.next();
                     String camelId = (String) mBeanServer.getAttribute(routeMBean, "CamelId");
                     if (camelId != null && camelId.equals(camelContext.getName())) {
                         RouteInfo ri = new RouteInfo(route, mBeanServer, routeMBean);
                         if (column.equalsIgnoreCase("ExchangesTotal")) {
                             routeMap.put(ri.getRouteId(), ri.getExchTotal());
                         } else if (column.equalsIgnoreCase("ExchangesCompleted")) {
                             routeMap.put(ri.getRouteId(), ri.getExchCompleted());
                         } else if (column.equalsIgnoreCase("ExchangesFailed")) {
                             routeMap.put(ri.getRouteId(), ri.getExchFailed());
                         } else if (column.equalsIgnoreCase("MinProcessingTime")) {
                             routeMap.put(ri.getRouteId(), ri.getMinProTime());
                         } else if (column.equalsIgnoreCase("MaxProcessingTime")) {
                             routeMap.put(ri.getRouteId(), ri.getMaxProTime());
                         } else if (column.equalsIgnoreCase("MeanProcessingTime")) {
                             routeMap.put(ri.getRouteId(), ri.getMeanProTime());
                         } else if (column.equalsIgnoreCase("LastProcessingTime")) {
                             routeMap.put(ri.getRouteId(), ri.getLastProTime());
                         }
                         routes.put(ri.getRouteId(), ri);
                     }
                }
 
            }
        }
        // Now lets sort
        routeMap = sortByValue(routeMap, !ordering);

        // Now lets print
        for (String ri : routeMap.keySet()) {
             RouteInfo row = routes.get(ri);
             row.prettyPrint();
        }
       
    }

    public Map sortByValue(Map map, boolean reverse) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
            }
        });

        if (reverse) {
            Collections.reverse(list);
        }

        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public void setCamelController(CamelController camelController) {
        this.camelController = camelController;
    }

    private boolean isColumnName(String name) {

        if (columns.contains(name)) {
            return true;
        }

        return false;
    }


    private class RouteInfo {

        private Long exchangesTotal; 
        private Long exchangesCompleted;
        private Long exchangesFailed;
        private Long minProcessingTime;
        private Long maxProcessingTime;
        private Long meanProcessingTime; 
        private Long totalProcessingTime;
        private Long lastProcessingTime;
        private Route route;
        private MBeanServer mBeanServer;

        public RouteInfo(Route route, MBeanServer mBeanServer, ObjectName routeMBean) throws Exception {
            this.route = route;
            this.mBeanServer = mBeanServer;
            exchangesTotal = (Long) mBeanServer.getAttribute(routeMBean, "ExchangesTotal");
            exchangesCompleted = (Long) mBeanServer.getAttribute(routeMBean, "ExchangesCompleted");
            exchangesFailed = (Long) mBeanServer.getAttribute(routeMBean, "ExchangesFailed");
            minProcessingTime = (Long) mBeanServer.getAttribute(routeMBean, "MinProcessingTime");
            maxProcessingTime = (Long) mBeanServer.getAttribute(routeMBean, "MaxProcessingTime");
            meanProcessingTime = (Long) mBeanServer.getAttribute(routeMBean, "MeanProcessingTime");
            totalProcessingTime = (Long) mBeanServer.getAttribute(routeMBean, "TotalProcessingTime");
            lastProcessingTime = (Long) mBeanServer.getAttribute(routeMBean, "LastProcessingTime");
        }

        public String getRouteId() {
            return route.getId();
        }

        public Long getExchTotal() {
            return exchangesTotal;
        }

        public Long getExchCompleted() {
            return exchangesCompleted;
        }

        public Long getExchFailed() {
            return exchangesFailed;
        }

        public Long getMinProTime() {
            return minProcessingTime;
        }

        public Long getMaxProTime() {
            return maxProcessingTime;
        }

        public Long getMeanProTime() {
            return meanProcessingTime;
        }

        public Long getTotalProTime() {
            return totalProcessingTime;
        }

        public Long getLastProTime() {
            return lastProcessingTime;
        }

        public void prettyPrint() {
            System.out.printf(" %15s %8d %8d %8d %8d %8d %8d %8s %8s%n",
                               route.getId(),
                               exchangesTotal, exchangesCompleted, exchangesFailed,
                               minProcessingTime, maxProcessingTime, meanProcessingTime,
                               totalProcessingTime, lastProcessingTime);
            System.out.println();
        }
    }
}
