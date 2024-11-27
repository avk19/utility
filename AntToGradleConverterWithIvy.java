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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // Parse properties and apply replacements
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
            
            // Process child nodes (echo, ant tasks, etc.)
            NodeList childNodes = target.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                Node childNode = childNodes.item(j);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element) childNode;
                    String childTag = childElement.getTagName();

                    if (childTag.equals("echo")) {
                        String message = childElement.getAttribute("message");
                        message = replaceProperties(message, antProperties);
                        gradleContent.append("        println '").append(message).append("'\n");
                    } else if (childTag.equals("ant")) {
                        String antFile = childElement.getAttribute("antfile");
                        if (!antFile.isEmpty()) {
                            gradleContent.append("    // Converted from <ant antfile=\"").append(antFile).append("\">\n");
                            gradleContent.append("    includeBuild '").append(antFile).append("'\n");

                            // Recursively process the referenced Ant file
                            String nestedGradleContent = convertAntToGradle(new File(xmlFile.getParent(), antFile).getAbsolutePath(), antProperties);
                            gradleContent.append(nestedGradleContent);
                        }
                    }
                    
                    // Replace attributes in all other tags recursively
                    replaceAttributesWithProperties(childElement, antProperties);
                }
            }
            gradleContent.append("    }\n");
            gradleContent.append("}\n\n");
        }

        return gradleContent.toString();
    }

    /**
     * Replace placeholders like ${propertyName} in the input string with values from the map.
     * 
     * @param input The string to process for replacements
     * @param antProperties The map of properties to use for replacement
     * @return The string with properties replaced
     */
    private static String replaceProperties(String input, Map<String, String> antProperties) {
        // Regex to match ${propertyName} in the input string
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String propertyValue = antProperties.getOrDefault(propertyName, matcher.group(0)); // default to original if not found
            matcher.appendReplacement(result, propertyValue);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Replace property placeholders in all attributes of an XML element.
     * 
     * @param element The XML element to process
     * @param antProperties The map of properties to use for replacement
     */
    private static void replaceAttributesWithProperties(Element element, Map<String, String> antProperties) {
        // Process each attribute in the element
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            Attr attribute = (Attr) element.getAttributes().item(i);
            String attributeValue = attribute.getValue();
            String updatedValue = replaceProperties(attributeValue, antProperties);
            attribute.setValue(updatedValue);
        }
    }
}
