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
$(document).ready(function(){

var expectError = false;
var client = $.reefClient({
    'server'     : 'http://127.0.0.1:8886',
    'error_function'  : function(errorString){
        ok(expectError, errorString);
        return errorString;
    },
    'auto_login' : {
        'name' : 'system',
        'password' : 'system'
    },
    'subscription_polling': {
        'enabled': true,
        'period': 10
    }
});


// ct is short for ContinueTest, with async callbacks we suspend the test suite running the next suite
// with the stop() call and will only restart after a start is called, using the always callback makes
// it easier to make sure start() will get called after the results are handled
var ct = function(promise){
    promise.always(function(){
        start();
    });
    return promise;
}

module("Basic Client Tests");

test("Failed Api calls are caught", function() {
    expect(4);
    stop();
    expectError = true;
    client.apiRequest({
        request: "madeUpFunctionName"
    }).fail(function(err){
        ok(err, "Got an error");
        ok(err.indexOf("madeUpFunctionName") != -1, "Error string includes functionName");
        expectError = false;
    }).done(function(result){
        ok(false, "done shouldn't be called back on a failure");
    }).always(function(){
        start();
        ok(true, "always should always be called");
    });
});

test("Logout and relogin", function() {
    expect(5);
    stop();
    client.logout().done(function(status){
        ok(true, "Logged out");
        expectError = true;
        client.login('unknownUser','badPassword').fail(function(errorMessage){
            ok(true, "Fail login of bad user");
            ct(client.login('system','system')).done(function(authToken){
                ok(true, "Logged back in");
                expectError = false;
                raises(function(){client.login('system','system')});
            });
        });
    });
});

test("Get Points", function() {
    expect(10);
    stop();
    client.getPoints().done(function(points){
        ok(points.length > 0, "More than 0 points");
        ok(points[0].name, "Points have a field called name");
        ok(points[0].type, "Points have a field called type");
        ok(points[0].unit, "Points have a field called unit");
        ok(points[0].uuid, "Points have a field called uuid");
    }).done(function(points){
        client.getEntityByUuid(points[0].uuid).done(function(entity){
            ok(entity.name, "Entitys have a field called name");
            ok(entity.uuid, "Entitys have a field called uuid");
            equal(entity.name, points[0].name, "Can get entity by uuid with matching name.")
        });
    }).done(function(points){
        client.findPointByName(points[0].name).done(function(point){
            ok(point !== undefined, "Can find point by name");
        });
        ct(client.findPointByName("UnknownPoint")).done(function(point){
            ok(point === undefined, "Undefined returned for OPTIONAL result");
        });
    });
});

test("Get Commands", function() {
    expect(4);
    stop();
    ct(client.getCommands()).done(function(commands){
        ok(commands.length > 0, "More than 0 commands");
        ok(commands[0].name, "Commands have a field called name");
        ok(commands[0].type, "Commands have a field called type");
        ok(commands[0].uuid, "Commands have a field called uuid");
    });
});

test("Get EntityChildren", function() {
    expect(9);
    stop();
    ct(client.getEntityChildrenFromTypeRoots("Equipment", "owns", 1, ["Point"])).done(function(equipments){
        ok(equipments.length > 0, "More than 0 Entities with type Equipment");
        ok(equipments[0].relations, "Entities have a field called relations");

        var relations = equipments[0].relations;
        ok($.isArray(relations), "Relations relation field is an Array");
        ok(relations.length > 0, "Atleast one set of relationships");

        equal(relations[0].descendant_of, true, "Relations have a field called descendant_of (true)");
        equal(relations[0].relationship, "owns", "Relations have a field called relationship (owns)");
        ok($.isArray(relations[0].entities), "Relations have a field called entities that is an array");

        var entities = relations[0].entities;
        ok(entities.length > 0, "More than one child Entity");
        ok($.inArray("Point",entities[0].types) != -1, "Related entities are Points");

        //    client.getEntityRelations(entities[0].uuid, "owns:*:false:Equipment").done(function(entity){
        //        start();
        //        ok(entity.name, "Entitys have a field called name");
        //        ok(entity.uuid, "Entitys have a field called uuid");
        //        equal(entity.name, points[0].name, "Can get entity by uuid with matching name.")
        //    });
    });
});

test("Polling Subscribe Api works", function() {
    expect(6);
    stop();
    client.subscribeToMeasurementsByNames(['SimulatedSubstation.Line01.Current']).done(function(subscriptionResult){
        var sub = subscriptionResult.subscription;
        var results = subscriptionResult.result;
        notEqual(sub, undefined);
        notEqual(results, undefined);
        var count = 3;
        sub.start( function(event, object){
            equal(event, 'MODIFIED', "Got subscription event");
            count = count - 1;
            if(count == 0) {
                sub.cancel();
            }
        }).done(function(reason){
            ok(reason.indexOf("cancel") !== -1, "Done is called after subscription is canceled");
            start();
        }).fail(function(errString){
            ok(false, "Subscription fail should not have been called.")
        });
    });
});

});