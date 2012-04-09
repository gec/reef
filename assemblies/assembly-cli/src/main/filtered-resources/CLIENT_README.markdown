
## Contents ##

This package contains a remote-cli that can connect to a running reef node.

## Instructions ##

To connect to a remote reef node it is necessary to edit a few of the files in the etc directory to "point" to the
correct messaging broker. Most users of deployed systems will be given a .zip/.tar.gz file that includes appropriate
files. These files are used to configure the client connection to the server. The same files will be used with other
applications that connect to reef.

- org.totalgrid.reef.amqp.cfg - contains broker port and ip address (and if using security the broker user/password)
- org.totalgrid.reef.user.cfg - contains the reef level username/password used during autologin (can be excluded if
  manually logging in or shared across users)
- trust-store.jks             - if using broker level SSL this file includes the server certificate for verification

## Documentation ##

See the documentation at http://code.google.com/p/reef/ for information on capabilities of the remote-cli.