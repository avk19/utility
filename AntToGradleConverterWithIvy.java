import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AntToGradleConverterWithIvy {

    private static final Set<String> processedFiles = new HashSet<>();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java AntToGradleConverterWithIvy <path to main build.xml> <path to output build.gradle> [key=value ...]");
            return;
        }

        String antBuildFile = args[0];
        String gradleBuildFile = args[1];

        // Parse additional property arguments
        Map<String, String> antProperties = new HashMap<>();
        for (int i = 2; i < args.length; i++) {
            String[] keyValue = args[i].split("=", 2);
            if (keyValue.length == 2) {
                antProperties.put(keyValue[0], keyValue[1]);
            }
        }

        try {
            String gradleContent = convertAntToGradle(antBuildFile, antProperties);
            Files.write(Paths.get(gradleBuildFile), gradleContent.getBytes());
            System.out.println("Conversion completed. Gradle build file created at: " + gradleBuildFile);
        } catch (Exception e) {
            System.err.println("Error during conversion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String convertAntToGradle(String antBuildFilePath, Map<String, String> antProperties) throws Exception {
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

        // Parse properties
        NodeList propertyNodes = document.getElementsByTagName("property");
        gradleContent.append("// Properties\n");
        for (int i = 0; i < propertyNodes.getLength(); i++) {
            Element property = (Element) propertyNodes.item(i);
            String name = property.getAttribute("name");
            String value = property.getAttribute("value");

            // Override property value if provided in antProperties
            if (antProperties.containsKey(name)) {
                value = antProperties.get(name);
            }

            if (!name.isEmpty() && value != null) {
                gradleContent.append("ext.").append(name).append(" = '").append(value).append("'\n");
            }
        }
        gradleContent.append("\n");

        // Parse targets
        NodeList targetNodes = document.getElementsByTagName("target");
        gradleContent.append("// Tasks\n");
        for (int i = 0; i < targetNodes.getLength(); i++) {
            Element target = (Element) targetNodes.item(i);
            String targetName = target.getAttribute("name");
            gradleContent.append("task ").append(targetName).append(" {\n");
            gradleContent.append("    doLast {\n");
            gradleContent.append("        println 'Executing ").append(targetName).append("'\n");
            gradleContent.append("    }\n");

            // Process <ant> tasks within this target
            NodeList childNodes = target.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                Node childNode = childNodes.item(j);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element) childNode;
                    if (childElement.getTagName().equals("ant")) {
                        String antFile = childElement.getAttribute("antfile");
                        if (!antFile.isEmpty()) {
                            gradleContent.append("    // Converted from <ant antfile=\"").append(antFile).append("\">\n");
                            gradleContent.append("    includeBuild '").append(antFile).append("'\n");

                            // Recursively process the referenced Ant file
                            String nestedGradleContent = convertAntToGradle(new File(xmlFile.getParent(), antFile).getAbsolutePath(), antProperties);
                            gradleContent.append(nestedGradleContent);
                        }
                    }
                }
            }
            gradleContent.append("}\n\n");
        }

        return gradleContent.toString();
    }
}
