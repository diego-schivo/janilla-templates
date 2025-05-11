# Janilla Website Template

This is a porting of [Payload Website Template](https://github.com/payloadcms/payload/tree/main/templates/website).

### How you can get started

> **_Note:_**  if you are unfamiliar with the terminal, you can set up the project in an IDE (section below).

Make sure you have Java SE Platform (JDK 24) and [Apache Maven](https://maven.apache.org/install.html) installed.

From the parent project root, run the following command to run the application:

```shell
mvn compile exec:exec -pl website
```

Then open a browser and navigate to <https://localhost:8443/>.

> **_Note:_**  consider checking the Disable Cache checkbox in the Network tab of the Web Developer Tools.

### Set up the project in an IDE

[Step-by-step Video Tutorial](https://youtu.be/OCH76YVurNs) available on [Janilla YouTube Channel](https://www.youtube.com/@janilla).

[Eclipse IDE](https://eclipseide.org/):

1. download the [Eclipse Installer](https://www.eclipse.org/downloads/packages/installer)
2. install the package for Enterprise Java and Web Developers with JRE 24
3. launch the IDE and choose Import projects from Git (with smart import)
4. select GitHub as the repository source, then search for `janilla-templates` and complete the wizard
5. open the Java class `com.janilla.templates.website.WebsiteTemplate` and launch Debug as Java Application
6. open a browser and navigate to <https://localhost:8443/>

> **_Note:_**  consider checking the Disable Cache checkbox in the Network tab of the Web Developer Tools.

### Where you can get help

Please visit [www.janilla.com](https://janilla.com/) for more information.

You can use [GitHub Issues](https://github.com/diego-schivo/janilla-templates/issues) to give or receive feedback.
