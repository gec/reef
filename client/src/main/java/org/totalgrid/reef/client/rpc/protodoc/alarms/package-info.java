/**
 * Copyright 2011 Green Energy Corp.
 * 
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/**
 * Proto definition file for Alarms.
 * 
 * <pre>
 * package org.totalgrid.reef.proto.Alarms;
 * 
 * import "Events.proto";
 * import "Model.proto";
 * 
 * option java_package = "org.totalgrid.reef.proto";
 * option java_outer_classname = "Alarms";
 * 
 * //  MAJOR TYPES IN THIS PROTO:
 * //
 * //  Alarm  -- An instance of an Alarm.
 * //  AlarmList   -- Used to get and subscribe to alarms
 * 
 * 
 * // USE CASES:
 * //
 * //  _______________________________________________________
 * //  Get the latest alarms.
 * //     - Default: select latest alarms with state != REMOVED
 * //     - Default: sort is reverse time order
 * //     - Default: record limit is 1000
 * //
 * //  ==> GET: AlarmList
 * //  { select: {} }
 * //
 * //  <== Result: AlarmList
 * //  { alarm:
 * //      //  uid  state                   id     event_type      time      severity subsystem user_id    entity
 * //      //  ===  =============           ====== =============   ========  ======== ========= ========   ==================
 * //      [ { 501, UNACK_SILENT,   event: { 1235,  "BreakerClose", 98273496, 2,       "FEP",    "system",  {"subst42.breaker23"} }},
 * //        { 500, ACKNOWLEDGED,  event: { 1234,  "BreakerOpen",  98273495, 2,       "FEP",    "system",  {"subst42.breaker23"} }}
 * //      ]
 * //  }
 * //
 * //  _______________________________________________________
 * //  Get the latest unacknowledged alarms with severity 2
 * //
 * //  ==> GET: AlarmList
 * //  { select: { state: [ UNACK_AUDIBLE, UNACK_SILENT ],
 * //              eventSelect: { severity: [2] }
 * //            }
 * //  }
 * //
 * //  <== Result: AlarmList
 * //  { alarm:
 * //      //  uid  state                   id     event_type      time      severity subsystem user_id    entity
 * //      //  ===  =============           ====== ==========      ========  ======== ========= ========   ==================
 * //      [ { 501, UNACK_SILENT,   event: { 1235,  "BreakerClose", 98273496, 2,       "FEP",    "system",  {"subst42.breaker23"} }}
 * //      ]
 * //  }
 * //
 * //
 * //  _______________________________________________________
 * //  Operator acknowledges a specific alarm.
 * //
 * //  ==> PUT: Alarm
 * //  { state: ACKNOWLEDGED,
 * //    event: { id: 1235}
 * //  }
 * //
 * //  <== Result: Alarm
 * //  { state: ACKNOWLEDGED,
 * //    event: {id: 12345
 * //            event_id: "BreakerClose",
 * //            ...
 * //           }
 * //  }
 * //
 * 
 * 
 * // An Alarm
 * //
 * message Alarm {
 * 
 *   // Alarm workflow states
 *   enum State {
 *     UNACK_AUDIBLE  = 1;  // Audible alarm not acknowledged by an Operator.
 *     UNACK_SILENT   = 2;  // Silent alarm not acknowledged by an Operator.
 *     ACKNOWLEDGED  = 3;  // Alarm acknowledged by Operator.
 *     REMOVED       = 4;  // Alarm not visible in standard Alarm List.
 *   }
 * 
 *   optional string   uid      = 1;  // UID of alarm is not equal to event.uid
 *   optional State    state    = 2;
 *   optional org.totalgrid.reef.proto.Events.Event event = 3;
 *   optional string   rendered = 4;  // This alarm rendered as a localized string.
 * }
 * 
 * 
 * // Use AlarmSelect within an AlarmList to get
 * // or subscribe to Alarms
 * //
 * message AlarmSelect {
 * 
 *   // EMPTY:
 *   // If AlarmSelect is empty, select all alarms with state != REMOVED
 * 
 *   // Select alarms with the specified states
 *   // If not present, select alarms with state != REMOVED.
 *   //
 *   repeated Alarm.State state =  1;
 * 
 *   // Select alarms by various event properties.
 *   // This is ANDed with any state selections above.
 *   //
 *   optional org.totalgrid.reef.proto.Events.EventSelect eventSelect =  2;
 * }
 * 
 * 
 * // Use AlarmList to query and subscribe to Alarms.
 * //
 * message AlarmList {
 *   optional AlarmSelect select  = 1 ;
 *   repeated Alarm  alarms  = 2 ;
 * }
 * 
 * 
 * // Configuration for each EventType. Configure event severity and whether a
 * // posted message is an alarm, event, or log.
 * // This is used internally within the server.
 * //
 * message EventConfig {
 * 
 *   enum Designation {
 *     ALARM = 1;
 *     EVENT = 2;
 *     LOG   = 3;
 *   }
 * 
 *   optional string           event_type    = 1 ; // Type of event: UserLogin, BreakerTrip, etc.
 *   optional uint32           severity      = 2 ; // 1 is most severe. Number of severities is configurable (default is 1-8).
 *   optional Designation      designation   = 3 ; // Designate this message type as Alarm, Event, or Log
 *   optional Alarm.State      alarm_state   = 4 ; // Initial alarm start state: UNACK_AUDIBLE, UNACK_SILENT, or ACKNOWLEDGED.
 *   optional string           resource      = 5 ; // Use this to render the message (including attributes)
 *   optional bool             built_in      = 6 ; // whether this message type is used by the system and cannot be deleted
 * }
 * </pre>
 */
package org.totalgrid.reef.client.rpc.protodoc.alarms;

