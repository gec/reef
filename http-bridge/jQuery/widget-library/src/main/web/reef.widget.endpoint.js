/*
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
/*
 This jQuery plugin is written following the best practices from:
   http://docs.jquery.com/Plugins/Authoring
 This code has been formatted and jLint tested using: http://jsfiddle.net
*/
(function($) {

    $.fn.displayEndpoints = function(options) {

        var displayError = function(msg) {
            setTargetDiv("<div class=\"endpoint_error\">" + msg + "</div>");
        };

        var setTargetDiv = function(text) {
            var header = "<div class=\"endpoint_widget\"><div class=\"endpoint_widget_header\">Server: " + settings.client.toString() + "</div>";
            var footer = "<div class=\"endpoint_widget_footer\">Powered by Greenbus.</div></div>";
            settings.target_div.html(header + text + footer);
        };

        var makeTd = function(fieldName, text, extraClasses) {
            if (extraClasses === undefined) extraClasses = "";
            return "<td class=\"endpoint_" + fieldName + extraClasses + "\">" + text + "</td>";
        };

        var makeEntry = function(endpointConnection, fieldName) {
            var extraClasses;
            switch (fieldName) {
            case "name":
                return makeTd(fieldName, endpointConnection.endpoint.name);
            case "state":
                if (endpointConnection.state === "COMMS_UP") extraClasses = " endpoint_state_ok";
                else extraClasses = " endpoint_state_bad";
                return makeTd(fieldName, endpointConnection.state, extraClasses);
            case "enabled":
                return makeTd(fieldName, endpointConnection.enabled, " endpoint_enabled_" + endpointConnection.enabled);
            case "button":
                if (endpointConnection.enabled) extraClasses = " endpoint_button_disable";
                else extraClasses = " endpoint_button_enable";
                return makeTd(fieldName, "toggle", extraClasses);
            default:
                throw "Unknown field name";
            }
        };

        var endpointRow = function(endpointConnection) {

            var tds = $.map(settings.display.field_names, function(fieldName) {
                return makeEntry(endpointConnection, fieldName);
            }).join("\n");

            return "<tr class=\"endpoint_row\" endpointUuid=\"" + endpointConnection.endpoint.uuid.value + "\">" + tds + "</tr>";
        };

        var headerRow = function() {
            return $.map(settings.display.field_names, function(fieldName) {
                return makeTd(fieldName, fieldName);
            }).join("\n");
        };

        var displayEndpointConnections = function(endpointConnections) {

            var tableRows = $.map(endpointConnections, function(endpointConnection) {
                return endpointRow(endpointConnection);
            }).join("\n");
            setTargetDiv("<table class=\"endpoint_table\">" + headerRow() + tableRows + "</table>");
        };

        var updateEndpoints = function() {
            settings.client.getEndpointConnections().done(function(endpointConnections) {
                settings.display_function(endpointConnections);
            }).fail(function(errorString) {
                settings.error_function(errorString);
            });
        };

        var settings = $.extend({
            // address of the server to query
            'client': undefined,
            // enables live-updates via polling, do not poll more often than once a second
            'polling': {
                'enabled': false,
                'period': 5000,
                'cancel_polling': function() {}
            },
            // allow overriding of the display and error routines
            // display_function takes a list of measurements with the enhanced fields ('value', 'quality_string', 'time_string' and 'abnormal')
            'display_function': displayEndpointConnections,
            // error message takes a string describing the error
            'error_function': displayError,
            // div we want to replace the contents of (if using default displayMeasurements)
            'target_div': this,

            'display': {
                'field_names': ['name', 'state', 'enabled', 'button']
            }
        }, options);

        if (settings.client === undefined) {
            throw "No client configured";
        }

        return this.each(function() {
            displayError("Connecting");
            $(settings.target_div).on("click", ".endpoint_button", function() {
                var buttonTd = $(this);

                var wasEnabled = buttonTd.hasClass("endpoint_button_disable");
                var endpointUuid = buttonTd.parent(".endpoint_row").attr('endpointUuid');

                var result;
                if (wasEnabled) result = settings.client.disableEndpointConnection(endpointUuid);
                else result = settings.client.enableEndpointConnection(endpointUuid);

                result.done(function() {
                    updateEndpoints();
                });
            });

            updateEndpoints();

            if (settings.polling && settings.polling.enabled && settings.polling.period) {
                var timer = setInterval(function() {
                    updateEndpoints();
                }, settings.polling.period);
                settings.polling.cancel_polling = function() {
                    timer.clearInterval();
                };
            }
        });

    };
})(jQuery);