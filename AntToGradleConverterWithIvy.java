import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

public class AntToGradleConverterWithIvy {

    private static final Set<String> processedFiles = new HashSet<>();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java AntToGradleConverterWithIvy <path to main build.xml> <path to output build.gradle>");
            return;
        }

        String antBuildFile = args[0];
        String gradleBuildFile = args[1];

        try {
            String gradleContent = convertAntToGradle(antBuildFile);
            Files.write(Paths.get(gradleBuildFile), gradleContent.getBytes());
            System.out.println("Conversion completed. Gradle build file created at: " + gradleBuildFile);
        } catch (Exception e) {
            System.err.println("Error during conversion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String convertAntToGradle(String antBuildFilePath) throws Exception {
        StringBuilder gradleContent = new StringBuilder();

        if (!processedFiles.add(antBuildFilePath)) {
            // Avoid re-processing the same file.
            return "";
        }

        File xmlFile = new File(antBuildFilePath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);

        Element projectElement = document.getDocumentElement();

        // Parse project attributes
        String projectName = projectElement.getAttribute("name");
        if (!projectName.isEmpty()) {
            gradleContent.append("rootProject.name = '").append(projectName).append("'\n\n");
        }

        // Parse included files
        NodeList includeNodes = document.getElementsByTagName("import");
        gradleContent.append("// Imported Builds\n");
        for (int i = 0; i < includeNodes.getLength(); i++) {
            Element include = (Element) includeNodes.item(i);
            String filePath = include.getAttribute("file");
            if (!filePath.isEmpty()) {
                String resolvedPath = resolvePath(antBuildFilePath, filePath);
                gradleContent.append(convertAntToGradle(resolvedPath));
            }
        }

        // Parse properties
        NodeList propertyNodes = document.getElementsByTagName("property");
        gradleContent.append("// Properties\n");
        for (int i = 0; i < propertyNodes.getLength(); i++) {
            Element property = (Element) propertyNodes.item(i);
            String name = property.getAttribute("name");
            String value = property.getAttribute("value");
            if (!name.isEmpty() && !value.isEmpty()) {
                gradleContent.append("ext.").append(name).append(" = '").append(value).append("'\n");
            }
        }
        gradleContent.append("\n");

        // Parse Ivy configuration
        gradleContent.append(parseIvyConfiguration(document));

        // Parse targets
        NodeList targetNodes = document.getElementsByTagName("target");
        gradleContent.append("// Tasks\n");
        for (int i = 0; i < targetNodes.getLength(); i++) {
            Element target = (Element) targetNodes.item(i);
            String targetName = target.getAttribute("name");
            if (!targetName.isEmpty()) {
                gradleContent.append("task ").append(targetName).append(" {\n");
                gradleContent.append("    doLast {\n");
                gradleContent.append("        println 'Executing ").append(targetName).append("'\n");
                gradleContent.append("    }\n");
                gradleContent.append("}\n\n");
            }
        }

        return gradleContent.toString();
    }

    private static String parseIvyConfiguration(Document document) {
        StringBuilder ivyContent = new StringBuilder();

        // Parse Ivy repositories
        NodeList ivySettingsNodes = document.getElementsByTagName("ivy:settings");
        ivyContent.append("// Repositories\n");
        ivyContent.append("repositories {\n");
        for (int i = 0; i < ivySettingsNodes.getLength(); i++) {
            Element ivySettings = (Element) ivySettingsNodes.item(i);
            String url = ivySettings.getAttribute("url");
            if (!url.isEmpty()) {
                ivyContent.append("    ivy {\n");
                ivyContent.append("        url '").append(url).append("'\n");
                ivyContent.append("    }\n");
            }
        }
        ivyContent.append("}\n\n");

        // Parse Ivy dependencies
        NodeList ivyResolveNodes = document.getElementsByTagName("ivy:resolve");
        ivyContent.append("// Dependencies\n");
        ivyContent.append("dependencies {\n");
        for (int i = 0; i < ivyResolveNodes.getLength(); i++) {
            Element ivyResolve = (Element) ivyResolveNodes.item(i);
            String org = ivyResolve.getAttribute("org");
            String module = ivyResolve.getAttribute("module");
            String rev = ivyResolve.getAttribute("rev");
            if (!org.isEmpty() && !module.isEmpty() && !rev.isEmpty()) {
                ivyContent.append("    implementation '").append(org).append(":").append(module).append(":").append(rev).append("'\n");
            }
        }
        ivyContent.append("}\n\n");

        return ivyContent.toString();
    }

    private static String resolvePath(String baseFilePath, String relativePath) {
        File baseFile = new File(baseFilePath);
        File parentDir = baseFile.getParentFile();
        File resolvedFile = new File(parentDir, relativePath);
        return resolvedFile.getAbsolutePath();
    }
}
