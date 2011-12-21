
## Compatibility Matrix

In general reef aims to maintain backwards binary compatibility between client and server versions within the same
minor revision. (Minor revision is middle number in the version number).  So all 0.4.0 clients should work
with all 0.4.x servers, all 0.5.x servers should be compatible with older 0.5.x clients etc.

The one big caveat is with functionality introduced inside a minor revision. In many cases (but not all)
new functionality in the client requires new code in the server so usually new functionality requires that both
the server and client be updated together. If the applications don't make use of newly introduced functionality
a newer client will work with an older server. We will try to mark which features require updated server components
here and in the change log.

<table>
  <tr>
    <th>Client/Server</th><th>0.3.x</th><th>0.4.0</th><th>0.4.1</th>
  </tr>
  <tr>
    <td>0.3.x</td><td>OK</td><td>-</td><td>-</td>
  </tr>
  <tr>
    <td>0.4.0</td><td>-</td><td>OK</td><td>OK</td>
  </tr>
  <tr>
    <td>0.4.1</td><td>-</td><td>OK*1</td><td>OK</td>
  </tr>
</table>

Caveats:
 1 - setpoint executions with strings and command error responses will be ignored

