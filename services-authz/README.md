
= Reef Security Model =

When determining if an operation can be performed in a system there are two questions that must be answered:

* "Who is asking me to do this?" - We want to know that if a request claims to originate with "Bob the Operator"
  that we trust it really is Bob making the request. This process is generally called Authentication and is implemented
  in a number of ways including username + password logins.
* "Can the requesting agent perform the action?" - Given that the Authentication service has verified who is making the
  request we need to make sure that agent is allowed to do any particular action. This enables granting of limited
  subsets of permissions to the systems users. A guest user can only read some data, whereas a operator can issue
  commands to affect the real world. This is known as Authorization.

> Because Authorization and Authentication look and sound very similar they often referred to as Auth-N and Auth-Z.

== Authentication (Auth-N) ==

Currently reef handles authentication using a standard username + password system. When a user has authenticated
successfully they are given an "Authentication Token" that needs to be included on all requests to the server. That
token must be well protected and only sent over secure (SSL) connections because an attacker with access to that token
can issue requests _as_ the agent who made the token.

In the future it will be possible to delegate the authentication to external systems such as LDAP, broker level users or
OpenId.

== Authorization (Auth-Z) ==

Handling per user authorization in reef is split into two parts. We define sets of permissions called "roles" that
describe whether a particular operation can be performed or not. These rules are based on four parts: allow, verb, resource
and selector. These roles are then assigned to agents, a single agent can be assigned many roles. If a user is given

* Allow : allow or deny - a rule can either be a "whitelist" (bob may do X) or a "blacklist" (bill cannot do Y). Both are
  useful for defining real world roles.
* Verb : read,create,update,delete,* - It is common that we will want to give users limited access to certain objects in
  the system. We may want to allow a operator user to view (read) all commands but not be able to add (create) or remove
  (delete) existing commands. Similarly we usually want to allow a user to change (update) their own password but not
  create new users or change which permissions they have.
* Resource : One or more resource ids describing which types of resources we are targeting.
* Selector : Rules that each object is checked against which can differentiate between allowed and denied objects within
  a type of object. Each object in the system may be associated with one or more entities, all of those entities must pass
  all selector checks or the overall request will fail. If no explicit selector is defined a wildcard selector (*) will
  be used that matches all resources.

=== Auth Process ===

1. Extract auth token from request headers, lookup associated roles
1. Lookup objects the request will affect
1. Filter out all objects the agent doesn't have read access to
1. Authorize action we are about to perform (create/update/delete) on remaining objects
  1. If any of the entities related to any of the objects is hidden or denied the whole transaction fails
1. Return result of request

=== Resource Ids ===

Resource Ids are string identifiers of the type of resource in question. The ids are generated from the Protobuf object
name using underscore_case_format.

* Low level ids that are not checked (batch_service, auth_request)
* Login and Agent Services (agent, agent_password, permission_set, auth_filter, auth_token)
* System Modeling Services (entity_edge, entity, entity_attributes, entity_attribute, event_config, command, endpoint
  config_file, comm_channel, point, calculation, trigger_set)
* Measurement Viewing (measurement_history, measurement_snapshot, measurement_statistics)
* Alarm and Event Viewing (event, alarm)
* Operational System Control (measurement_batch, command_lock, user_command_request, meas_override, endpoint_connection,
  endpoint_state, endpoint_enabled, front_end_processor, measurement_processing_connection)
* Application Lifecycle (application_config, status_snapshot)

== Features ==

* Actions that are not allowed recieve an explicit error message indicating what permission you would need to perform
  the action. This enables quickly troubleshooting and updating the roles when modeling a new role.

```
karaf@root> command:issue C1

Error running command: org.totalgrid.reef.client.exception.UnauthorizedException: Couldn't get command execution lock for: Li
st(C1) - Denied(true,No permission matched command_lock:create. Assuming deny *)
```

* When searching objects you would not be authorized to view are removed from the results. This filtering also means
  searching for an object you are not authorized to see will return an empty result rather than an error message. This
  means a limited user cannot differentiate between an object they can't see and one that is not in the system.

== Limitations ==

* Deletes, creates and updates do not filter results before checking authorization. This means a "delete *" operation
  performed by a limited user will generally fail even if a "view *" shows only deletable objects.
* If a user is able to create objects with arbitrary names they can use brute force to determine what objects exist in
  the system by creating objects and looking for collisions (which result in failures).
* Subscriptions shouldn't be allowed when using per-resource security as only the initial "binding" is checked and if a
  wildcard subscription is done the user would see every resource that matched the subscription request regardless of
  whether it should be visible.
* Subscriptions are not directly authorizable, they are treated the same as a read.










