package com.tribune.devtools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class JsonController implements Initializable {

    private final static Logger log = Logger.getLogger(JsonController.class.getName());

    private static final String INITIAL_INDENTION = "   ";
    private static final String STYLE_PROP_NAME = "-fx-fill: darkgreen; -fx-font-weight: bold";
    private static final String STYLE_BRACKET = "-fx-fill: red; -fx-font-weight: bold";
    private static final String STYLE_BRACE = "-fx-fill: blue; -fx-font-weight: bold";
    private static final String STYLE_NUMBER = "-fx-fill: blue";
    private static final String STYLE_STRING = "-fx-fill: brown";
    private static final String STYLE_ERROR = "-fx-fill: red; -fx-font-size: large; -fx-font-weight: bold";
    private static final Duration DEBOUNCE_DELAY = Duration.millis(300);
    private static final Duration TOOLTIP_DELAY = Duration.millis(200);

    @FXML
    private TextArea textArea;

    @FXML
    private TextFlow textFlow;

    private ObjectNode output;
    private PauseTransition debounceTimer;
    private Animation currentAnimation;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        debounceTimer = new PauseTransition(DEBOUNCE_DELAY);
        debounceTimer.setOnFinished(ignored -> {
            String input = textArea.getText();
            beautifyJson(input);
        });

        textArea.textProperty().addListener((ignored1, ignored2, ignored3) -> {
            debounceTimer.stop();
            debounceTimer.playFromStart();
        });
    }


    @FXML
    protected void beautifyJson(String input) {
        if (input == null || input.trim().isEmpty()) {
            textFlow.getChildren().clear();
            output = null;
            return;
        }

        try {
            output = mapper.readValue(input, ObjectNode.class);
            textFlow.getChildren().clear();
            doSomeMagic(output, null, INITIAL_INDENTION, textFlow);
        } catch (JsonProcessingException ex) {
            handleJsonBoardError(ex, textArea, textFlow);
        }
    }

    private void handleJsonBoardError(JsonProcessingException ex, TextArea jsonFormatInput, TextFlow textFlow) {
        log.warning("ERROR: " + ex.getMessage());

        String inputText = jsonFormatInput.getText();
        if (inputText.isEmpty()) {
            textFlow.getChildren().clear();
            return;
        }

        if (!inputText.startsWith("{")) {
            Text invalidText = new Text(inputText);
            invalidText.setFill(Color.RED);
            textFlow.getChildren().setAll(invalidText);
            return;
        }

        int errorOffset = calculateErrorOffset(ex, inputText);
        errorOffset = Math.clamp(errorOffset, 0, inputText.length() - 1);
        errorPosition = errorOffset;

        try {
            char errorChar = inputText.charAt(errorOffset);
            Text before = new Text(inputText.substring(0, errorOffset));
            Text error = new Text(String.valueOf(errorChar));
            error.setStyle(STYLE_ERROR);

            String errorTooltip = createErrorTooltip(ex, errorChar);
            Tooltip tooltip = new Tooltip(errorTooltip);
            tooltip.setShowDelay(TOOLTIP_DELAY);
            tooltip.setWrapText(true);
            tooltip.setPrefWidth(300);
            Tooltip.install(error, tooltip);

            Text after = new Text(inputText.substring(Math.min(errorOffset + 1, inputText.length())));
            textFlow.getChildren().clear();
            textFlow.getChildren().addAll(before, error, after);
            playAnimation(error);
        } catch (StringIndexOutOfBoundsException e) {
            log.warning("Error rendering JSON error: " + e.getMessage());
            Text errorText = new Text("JSON Parsing Error at position " + errorOffset);
            errorText.setFill(Color.RED);
            textFlow.getChildren().setAll(errorText);
        }
    }

    private String createErrorTooltip(JsonProcessingException ex, char errorChar) {
        String baseMessage = ex.getOriginalMessage();
        if (baseMessage == null) {
            baseMessage = "Unknown JSON parsing error";
        }

        baseMessage = baseMessage.replaceAll(", code .?\\d+", "")
                .replaceAll(" \\(code .*?\\d+\\)", "");

        return "JSON Syntax Error\n\n" +
                "Problem: " + baseMessage + "\n\n" +
                "Error at: '" + errorChar + "' (Position " + getErrorPosition() + ")";
    }

    private int errorPosition = 0;

    private int getErrorPosition() {
        return errorPosition;
    }

    private int calculateErrorOffset(JsonProcessingException ex, String inputText) {
        int errorOffset = (int) ex.getLocation().getCharOffset() - 1;

        if (errorOffset < 0 || errorOffset >= inputText.length()) {
            return 0;
        }

        char errorChar = inputText.charAt(errorOffset);
        String errorMessage = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        // For unexpected character errors, point to the problematic character
        int clamp = Math.clamp(errorOffset, 0, inputText.length() - 1);
        if (errorMessage.contains("unexpected character") ||
                errorMessage.contains("expected") ||
                errorMessage.contains("unexpected end")) {
            return clamp;
        }

        // For missing comma errors, point to the character after which comma is missing
        if (errorMessage.contains("missing")) {
            if (errorChar == '\n' || errorChar == ' ') {
                // Go back to find the actual token
                for (int i = errorOffset - 1; i >= 0; i--) {
                    char ch = inputText.charAt(i);
                    if (!Character.isWhitespace(ch)) {
                        return i;
                    }
                }
            }
            return clamp;
        }

        // For quote errors (code 34)
        if (errorMessage.contains("code 34") || errorMessage.contains("quote")) {
            if (Character.isLetterOrDigit(errorChar)) {
                return errorOffset;
            }
            if (errorOffset > 0 && errorChar == '"') {
                return errorOffset;
            }
        }

        // For newline or colon, point to the token before it
        if (errorChar == '\n' || errorChar == ':' || errorChar == ' ') {
            for (int i = errorOffset - 1; i >= 0; i--) {
                char ch = inputText.charAt(i);
                if (!Character.isWhitespace(ch)) {
                    return i;
                }
            }
            return Math.max(0, errorOffset - 1);
        }

        // Default: return the exact error location
        return clamp;
    }

    private void playAnimation(Node error) {
        if (currentAnimation != null) {
            currentAnimation.stop();
        }

        FadeTransition ft = new FadeTransition(Duration.seconds(1), error);
        ft.setFromValue(1.0);
        ft.setToValue(0.1);

        ScaleTransition st = new ScaleTransition(Duration.seconds(1), error);
        st.setFromX(0.75);
        st.setToX(1.5);
        st.setFromY(0.75);
        st.setToY(1.5);

        ParallelTransition pt = new ParallelTransition(error, ft, st);
        pt.setCycleCount(Animation.INDEFINITE);
        pt.setAutoReverse(true);
        currentAnimation = pt;
        pt.play();
    }


    private void doSomeMagic(JsonNode node, String nodeName, String indention, TextFlow textFlow) throws JsonProcessingException {
        if (nodeName != null) {
            Text jsonObjectPropName = new Text(indention + "\"" + nodeName + "\"");
            jsonObjectPropName.setStyle(STYLE_PROP_NAME);
            Text colon = new Text(" : ");
            textFlow.getChildren().addAll(jsonObjectPropName, colon);
            indention = indention.concat(indention);
        }

        if (node.isArray()) {
            renderArray(node, indention, textFlow);
        } else {
            renderObject(node, indention, textFlow);
        }
    }

    private void renderArray(JsonNode node, String indention, TextFlow textFlow) throws JsonProcessingException {
        Text startBrackets = new Text("[");
        startBrackets.setStyle(STYLE_BRACKET);
        textFlow.getChildren().add(startBrackets);

        ArrayNode arrayNode = (ArrayNode) mapper.readTree(node.toString());
        Iterator<JsonNode> iterator = arrayNode.iterator();

        while (iterator.hasNext()) {
            JsonNode child = iterator.next();
            doSomeMagic(child, null, indention, textFlow);
            if (iterator.hasNext()) {
                textFlow.getChildren().add(new Text(","));
            }
        }

        Text endBrackets = new Text("]");
        endBrackets.setStyle(STYLE_BRACKET);
        textFlow.getChildren().add(endBrackets);
    }

    private void renderObject(JsonNode node, String indention, TextFlow textFlow) throws JsonProcessingException {
        Text startBraces = new Text("{\n");
        startBraces.setStyle(STYLE_BRACE);
        textFlow.getChildren().add(startBraces);

        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> field = iterator.next();
            renderField(field, indention, textFlow, iterator.hasNext());
        }

        String closingIndention = INITIAL_INDENTION.equals(indention) ? "" : indention;
        Text endBraces = new Text("\n" + closingIndention + "}");
        endBraces.setStyle(STYLE_BRACE);
        textFlow.getChildren().add(endBraces);
    }

    private void renderField(Map.Entry<String, JsonNode> field, String indention, TextFlow textFlow, boolean hasNext) throws JsonProcessingException {
        if (field.getValue().isObject() || field.getValue().isArray()) {
            doSomeMagic(field.getValue(), field.getKey(), indention, textFlow);
        } else {
            Text propertyName = new Text(indention + "\"" + field.getKey() + "\"");
            propertyName.setStyle(STYLE_PROP_NAME);

            Text colon = new Text(" : ");
            Text propertyValue = new Text(String.valueOf(field.getValue()));

            if (field.getValue().isNumber()) {
                propertyValue.setStyle(STYLE_NUMBER);
            } else {
                propertyValue.setStyle(STYLE_STRING);
            }

            textFlow.getChildren().addAll(propertyName, colon, propertyValue);
        }

        if (hasNext) {
            textFlow.getChildren().add(new Text(",\n"));
        }
    }

    @FXML
    protected void copy() {
        if (output == null) {
            log.warning("No valid JSON to copy");
            return;
        }

        try {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(Map.of(DataFormat.PLAIN_TEXT, output.toPrettyString()));
        } catch (Exception e) {
            log.warning("Failed to copy to clipboard: " + e.getMessage());
        }
    }

}