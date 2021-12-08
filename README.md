
# PackageBuilder

This is a tool for salesforce.com. It can do one of two things:
* connect to an org and generate a package.xml that can subsequently be used with the Force.com Migration Tool to extract code and metadata from an org.
* examine a directory containing an unzipped Force.com Migration Tool package and generate a package.xml 


### Usage:
``` 
usage: java -jar PackageBuilder.jar [-w basedirectory] [-b <parameter
            file1>] [-u <SF username>] [-p <SF password>]
 -b,--buildprops <arg>          file containing org parameters (see below)
 -c,--connectprops <arg>        file containing environment connection
                                parameters. sf.username, sf.password,
                                sf.apiversion, sf.serverurl(see below)
 -a,--sf.apiversion <arg>       api version to use, will default to 53.0
                                sf.apiversion in property file
 -s,--sf.serverurl <arg>        server URL for the org
                                (https://login.salesforce.com)
                                sf.serverurl in property file
 -p,--sf.password <arg>         password for the org (t0pSecr3t)
                                sf.password in property file
 -q,--sf.token <arg>            token for the org (t0pSecr3t)
                                sf.password in property file
 -u,--sf.username <arg>         username for the org
                                (someuser@someorg.com)
                                sf.username in property file"
 -t,--destination <arg>         directory where the generated package.xml
                                will be written
 -m,--metadatatargetdir <arg>   Directory to download meta data source
                                (different to where package.xml will go)
                                to
 -o,--loglevel <arg>            output log level (INFO, FINE, FINER make
                                sense) - defaults to INFO if not provided
 -d,--download                  directly download assets, removing the
                                need for ANT or MDAPI call
 -g,--gitcommit                 commits the changes to git. Requires -d -c
                                options
 -l,--localonly                 Don't re-download package.zip files, but
                                process existing ones
 -f,--unzip                     unzip any retrieved package(s)
 -w,--basedirectory <arg>       base directory from which to generate
                                package.xml
 -k,--retaintargetdir           do not clear the metadatatargetdir
                                provided when unzipping
```
#### Filtering what goes in the package



When filtering by item name, the PackageBuilder will create an artificial name by prepending the metadata type to the actual item name, so e.g. the Opportunity field My_field__c will be represented internally as `CustomField:Opportunity.My_field__c`. This enables writing more precise patterns like `CustomField:Opportunity.*` which will filter all custom fields on the Opportunity object, as opposed to the pattern `.*Opportunity.*` which would match anything and everything associated with the Opportunity object and beyond (e.g. an Account field called Parent_Opportunity__c).

Multiple patterns can be provided separated by commas. Unpredictable behavior if your pattern includes commas.


This filter works against the email/name of the user who is the last to have modified a given item.



All parameters can be provided in parameter files specified with the -o parameter. More than one file can be provided (as in the example below, where one file would define what to fetch, skippatterns, etc., and the other where to fetch from). If any parameters are provided both in files and on the command line, the command line ones will be used. 

##### Property file format
The property files use standard Java property file format, i.e. `parameter=value`. E.g.

```property
# equivalent to -a commandline parameter
apiversion=44.0
# equivalent to -mi commandline parameter
metadataitems=ApexClass, ApexComponent, ApexPage
# equivalent to -s commandline parameter
sf.serverurl=https://login.salesforce.com
# equivalent to -u commandline parameter
sf.username=my@user.name
# equivalent to -p commandline parameter
sf.password=t0ps3cr3t
# equivalent to -sp commandline parameter
skipItems=.*fflib_.*,.*Class:AWS.*,ApexPage.*
# equivalent to -d commandline parameter
targetdirectory=src
```

#### Example: 

#### Use of changedata parameter
The changedata parameter will augment the generated package.xml file with data about who/when last changed the given metadata item. So instead of getting 
```
<name>CustomField</name>
<members>Account.Active__c</members>
<members>Account.CustomerPriority__c</members>
```
you will get
```
<name>CustomField</name>
<members lastmodified="2018-08-30T09:28:58" lastmodifiedby="Kim Galant"  lastmodifiedemail="kim.galant@salesforce.com">Account.Active__c</members>
<members lastmodified="2018-08-30T09:28:58" lastmodifiedby="Kim Galant"  lastmodifiedemail="kim.galant@salesforce.com">Account.CustomerPriority__c</members>
```
Note that this adds a lastmodified attribute which contains the last change date of that item, the name and email of the user who changed it (from the SF User table).
If this package.xml file is used for a retrieve, Salesforce (as of API 44) will happily ignore the additional attributes. They are added to help provide additional insight about who last touched each individual item.
