
Reef Javascript Support
=========================

If the server has the http-bridge installed it means we can use any Http client to pull data from the server. In
particular it makes it very simple to access a small view into the data using javascript.

Widgets
========

We have provided a small set of simple, embeddable, javascript widgets that allow external applications to embedded
ui components that expose different aspects of the running system.

The widgets are written as jQuery plugins and assume that jQuery will have be loaded before they are imported. The
widgets have been developed against jQuery 1.4.3 but since we don't use advanced jQuery features it should be compatible
with most versions of jQuery.

Each widget is generally designed to be called on a DOM object which is the container of the resultant data. The data
provided by the plugins will generally be a set of <div> elements that describe their content using CSS classes. This
should allow the embedding page to customize the look and feel of the widgets without needing to edit the plugin code.
In general the CSS files provided with the widgets should be considered more as documentation of the css classes rather
than a finished product.

Where ever possible the widgets should allow delegating the displaying of the data to an external function so a page can
render the data exactly as desired.

Widgets are tested and should work against most modern web browsers. (Chrome >= 15, Internet Explorer >= 7, Firefox >= 3)

[Minimal Widget Demo](js-service-client/src/main/web/reef.widget-demos.html)

Current Value Widget
---------------------

This widget is useful for showing the state of a known set of Measurements in an external web page. A page using this
widget must import jQuery and reef.widget.measurement.js and will want to provide CSS definitions for the the classes
in reef.widget.measurement.css. Below is the simplest invocation for the displayMeasurements routine but there are many
other options that can be overridden (see source for details).

```javascript
$(document).ready(function(){
    $('#data_div').displayMeasurements({
        'server'     : 'https://127.0.0.1:8886',
        'point_names' : [
            'SimulatedSubstation.Line01.Current',
            'StaticSubstation.Line02.Current',
            'SimulatedSubstation.Breaker01.Bkr',
            'StaticSubstation.Breaker02.Bkr',
        ]
    });
});
```