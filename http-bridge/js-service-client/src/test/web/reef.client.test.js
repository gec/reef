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

var client = $.reefClient({
    'server'     : 'http://127.0.0.1:8886',
    'error_function'  : function(errorString){
        start();
        ok(false, errorString);
    },
    'autoLogin' : {
        'name' : 'system',
        'password' : 'system'
    }
});

module("Basic Client Tests");

test("Failed Api calls are caught", function() {
    expect(2);
    stop();
    client.apiRequest({
        request: "madeUpFunctionName"
    }).fail(function(err){
        start();
        ok(err, "Got an error");
        ok(err.indexOf("madeUpFunctionName") != -1, "Error string includes functionName");
    });
});


test("Get Points", function() {
    expect(8);
    stop();
    client.getPoints().done(function(points){
        ok(points.length > 0, "More than 0 points");
        ok(points[0].name, "Points have a field called name");
        ok(points[0].type, "Points have a field called type");
        ok(points[0].unit, "Points have a field called unit");
        ok(points[0].uuid, "Points have a field called uuid");
        client.getEntityByUuid(points[0].uuid).done(function(entity){
            start();
            ok(entity.name, "Entitys have a field called name");
            ok(entity.uuid, "Entitys have a field called uuid");
            equal(entity.name, points[0].name, "Can get entity by uuid with matching name.")
        });
    });
});

test("Get Commands", function() {
    expect(4);
    stop();
    client.getCommands().done(function(commands){
        start();
        ok(commands.length > 0, "More than 0 commands");
        ok(commands[0].name, "Commands have a field called name");
        ok(commands[0].type, "Commands have a field called type");
        ok(commands[0].uuid, "Commands have a field called uuid");
    });
});

test("Get EntityChildren", function() {
    expect(9);
    stop();
    client.getEntityChildrenFromTypeRoots("Equipment", "owns", 1, ["Point"]).done(function(equipments){
        start();
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
        ok($.inArray("Point",entities[0].types), "Related entities are Points");

        //    client.getEntityRelations(entities[0].uuid, "owns:*:false:Equipment").done(function(entity){
        //        start();
        //        ok(entity.name, "Entitys have a field called name");
        //        ok(entity.uuid, "Entitys have a field called uuid");
        //        equal(entity.name, points[0].name, "Can get entity by uuid with matching name.")
        //    });
    });
});

});