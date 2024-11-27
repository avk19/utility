import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class AntToGradleConverterWithIvy {

    private static final Set<String> processedFiles = new HashSet<>();

    import java.util.HashMap;
import java.util.Map;

public static void main(String[] args) {
    if (args.length < 2) {
        System.out.println("Usage: java AntToGradleConverterWithIvy <path to main build.xml> <path to output build.gradle> [key=value ...]");
        return;
    }

    String antBuildFile = args[0];
    String gradleBuildFile = args[1];

    // Parse properties
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

    // (Remaining logic for targets and nested antfile processing stays the same...)

    return gradleContent.toString();
}


    
}
