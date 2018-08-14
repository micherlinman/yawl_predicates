/*
 * Copyright (c) 2004-2012 The YAWL Foundation. All rights reserved.
 * The YAWL Foundation is a collaboration of individuals and
 * organisations who are committed to improving workflow technology.
 *
 * This file is part of YAWL. YAWL is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation.
 *
 * YAWL is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with YAWL. If not, see <http://www.gnu.org/licenses/>.
 */

package org.yawlfoundation.yawl.resourcing.datastore.eventlog;

import org.yawlfoundation.yawl.logging.XESTimestampComparator;
import org.yawlfoundation.yawl.logging.YXESBuilder;
import org.yawlfoundation.yawl.util.XNode;
import org.yawlfoundation.yawl.util.XNodeParser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Author: Michael Adams
 * Creation Date: 30/04/2010
 */
public class ResourceXESLog extends YXESBuilder {

    public ResourceXESLog() { super(); }


    protected void processEvents(XNode root, XNode yawlCases) {
        List<XNode> caseNodes = yawlCases.getChildren();
        for (XNode yawlCase : caseNodes) {

            // ignore cases with no events
            if (yawlCase.hasChildren()) {
                XNode trace = root.addChild(traceNode(yawlCase.getAttributeValue("id")));
                processCaseEvents(yawlCase, trace);
            }
        }
    }


    protected String translateEvent(String yEvent) {
        String xesEvent;
        switch (EventLogger.event.valueOf(yEvent)) {
            case allocate             : xesEvent = "assign";     break;
            case start                : xesEvent = "start";      break;
            case complete             : xesEvent = "complete";   break;
            case suspend              : xesEvent = "suspend";    break;
            case deallocate           : xesEvent = "withdraw";   break;
            case delegate             : xesEvent = "reassign";   break;
            case reallocate_stateful  : xesEvent = "reassign";   break;
            case reallocate_stateless : xesEvent = "reassign";   break;
            case skip                 : xesEvent = "manualskip"; break;
            case resume               : xesEvent = "resume";     break;
            case cancel               : xesEvent = "ate_abort";  break;
            case timer_expired        : xesEvent = "ate_abort";  break;
            case cancel_case          : xesEvent = "pi_abort";   break;
            case cancelled_by_case    : xesEvent = "pi_abort";   break;
            default                   : xesEvent = "unknown";
        }
        return xesEvent;
    }


    protected String getComment() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        return "Generated by the YAWL Resource Service " +
                df.format(new Date(System.currentTimeMillis()));
    }


    public String mergeLogs(XNode rsRawCases, String engCasesStr) {
        XNode engCases = new XNodeParser().parse(engCasesStr);
        XNode rsCases = buildLogNode(rsRawCases);
        if (hasEvents(engCases) && hasEvents(rsCases)) {
            return mergeLogs(engCases, rsCases).toPrettyString(true);
        }
        else if (hasEvents(engCases)) {
            return engCasesStr;
        }
        else if (hasEvents(rsCases)) {
            return rsCases.toPrettyString(true);
        }
        else return null;
    }


    /******************************************************************************/

    private void processCaseEvents(XNode yawlCase, XNode trace) {
        List<XNode> events = yawlCase.getChildren();
        if (events != null) {
            for (XNode yEvent : events) {
                trace.addChild(eventNode(yEvent));
            }
        }
    }


    private XNode eventNode(XNode yEvent) {
        XNode eventNode = new XNode("event");
        eventNode.addChild(dateNode("time:timestamp", yEvent.getChildText("timestamp")));
        eventNode.addChild(stringNode("concept:name", yEvent.getChildText("taskname")));
        eventNode.addChild(stringNode("lifecycle:transition",
                translateEvent(yEvent.getChildText("descriptor"))));
        eventNode.addChild(stringNode("concept:instance", yEvent.getChildText("instanceid")));
        eventNode.addChild(stringNode("org:resource", yEvent.getChildText("resource")));
        return eventNode;
    }


    private XNode buildLogNode(XNode events) {
        if (events != null) {
            XNode root = new XNode("root");
            processEvents(root, events);
            return root;
        }
        return null;
    }


    private boolean hasEvents(XNode node) {
        return (node != null) && (node.getChild("trace") != null);
    }


    // use the engine log as the base, and add resource events to it as required
    private XNode mergeLogs(XNode engLog, XNode rsLog) {
        Map<String, XNode> rsCaseMap = buildCaseMap(rsLog);
        for (XNode trace : engLog.getChildren("trace")) {
            String caseID = trace.getChild("string").getAttributeValue("value");
            if (rsCaseMap.containsKey(caseID)) {
                mergeTraces(trace, rsCaseMap.get(caseID));
                trace.sort(new XESTimestampComparator());
            }
        }
        engLog.insertComment(1, "and then merged with the Resource Service log");
        return engLog;
    }


    private Map<String, XNode> buildCaseMap(XNode logNode) {
        Map<String, XNode> caseMap = new Hashtable<String, XNode>();
        for (XNode trace : logNode.getChildren()) {  // rs log has 'trace' children only
            String caseID = trace.getChild("string").getAttributeValue("value");
            caseMap.put(caseID, trace);
        }
        return caseMap;
    }


    private void mergeTraces(XNode master, XNode slave) {
        for (XNode event : slave.getChildren("event")) {
            String transition = getTransition(event);
            if (transition != null) {

                // choose which event to use for duplicated events
                if (mergeableEvent(transition)) {               // start or complete
                    mergeEvent(master, event, transition);
                }
                else if (slaveHasPrecedence(transition)) {
                    removeEvent(master, getTaskName(event), getInstanceID(event), transition);
                }

                // otherwise just add it
                if (! masterHasPrecedence(transition)) {
                    master.addChild(event);
                }    
            }
        }
    }


    private String getEventValue(XNode event, String key) {
        for (XNode node : event.getChildren("string")) {
            if (node.getAttributeValue("key").equals(key)) {
                return node.getAttributeValue("value");
            }
        }
        return null;
    }

    
    private String getTransition(XNode event) {
        return getEventValue(event, "lifecycle:transition");
    }


    private String getTaskName(XNode event) {
        return getEventValue(event, "concept:name");
    }


    private String getInstanceID(XNode event) {
        return getEventValue(event, "concept:instance");
    }


    private String getOrgResource(XNode event) {
        return getEventValue(event, "org:resource");
    }


    // These transitions appear in both logs, but each has relevant data, so they
    // have to be merged by individual elements
    private boolean mergeableEvent(String transition) {
        return transition.equals("start") || transition.equals("complete");
    }


    // These transitions appear in both logs - the resource service will take
    // precedence since it contains the resource that triggered the event
    private boolean slaveHasPrecedence(String transition) {
        return transition.equals("suspend") || transition.equals("resume");
    }


    // These transitions describe cancelled items, so the engine log is used
    // for completeness
    private boolean masterHasPrecedence(String transition) {
        return mergeableEvent(transition) ||
                transition.equals("ate_abort") || transition.equals("pi_abort") ;
    }


    private void removeEvent(XNode node, String taskName, String instanceID, String transition) {
        XNode toRemove = null;
        for (XNode event : node.getChildren("event")) {
            if (getTransition(event).equals(transition) &&
                    getInstanceID(event).equals(instanceID) &&
                    getTaskName(event).equals(taskName)) {
                toRemove = event;
                break;
            }
        }
        if (toRemove != null) node.removeChild(toRemove);
    }


    private void mergeEvent(XNode node, XNode rsEvent, String transition) {
        String instanceID = getInstanceID(rsEvent);
        String taskName = getTaskName(rsEvent);
        String orgResource = getOrgResource(rsEvent);
        if (orgResource == null) return;

        for (XNode event : node.getChildren("event")) {
            if (getTransition(event).equals(transition) &&
                    getInstanceID(event).equals(instanceID) &&
                    getTaskName(event).equals(taskName)) {
                int pos = event.posChildWithAttribute("key", "concept:instance");
                if (pos > -1) {
                    event.insertChild(pos + 1, stringNode("org:resource", orgResource));
                }
                else event.addChild(stringNode("org:resource", orgResource));
                break;
            }
        }
    }

}
