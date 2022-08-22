package com.zatribune.devtools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

public class JsonController implements Initializable {

    private static final String INITIAL_INDENTION="   ";
    @FXML
    private TextArea textArea;

    @FXML
    private TextFlow textFlow;

    private ObjectNode output;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        textArea.textProperty().addListener((observable, oldValue, newValue) ->
                beautifyJson(newValue));
    }


    private void beautifyJson(String input) {

        try {
            output = mapper.readValue(input, ObjectNode.class);
            textFlow.getChildren().clear();
            doSomeMagic(output, null, INITIAL_INDENTION, textFlow);
        } catch (Exception ignored) {
        }
    }

    private void doSomeMagic(JsonNode node, String nodeName, String indention, TextFlow textFlow) throws JsonProcessingException {


        if (nodeName != null) {
            Text jsonObjectPropName = new Text(indention + "\"" + nodeName + "\"");
            jsonObjectPropName.setStyle("-fx-fill: darkgreen;-fx-font-weight: bold");
            Text colon = new Text(" : ");
            textFlow.getChildren().addAll(jsonObjectPropName, colon);
            indention = indention.concat(indention);
        }

        if (node.isArray()) {
            Text startBrackets = new Text("[");
            startBrackets.setStyle("-fx-fill: red;-fx-font-weight: bold");
            textFlow.getChildren().add(startBrackets);
            ArrayNode arrayNode = (ArrayNode) mapper.readTree(node.toString());
            Iterator<JsonNode> iterator = arrayNode.iterator();
            while (iterator.hasNext()) {
                JsonNode child = iterator.next();
                doSomeMagic(child, null, indention, textFlow);
                if (iterator.hasNext())
                    textFlow.getChildren().add(new Text(","));
            }
            Text endBrackets = new Text("]");
            endBrackets.setStyle("-fx-fill: red;-fx-font-weight: bold");
            textFlow.getChildren().add(endBrackets);
            return;//really important
        }

        Text startBraces = new Text("{\n");
        startBraces.setStyle("-fx-fill: blue;-fx-font-weight: bold");
        textFlow.getChildren().add(startBraces);

        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> field = iterator.next();
            if (field.getValue().isObject() || field.getValue().isArray()) {
                doSomeMagic(field.getValue(), field.getKey(), indention, textFlow);
            } else {
                Text propertyName = new Text(indention + "\"" + field.getKey() + "\"");
                propertyName.setStyle("-fx-fill: green");
                Text propertyValue = new Text(String.valueOf(field.getValue()));
                if (field.getValue().isNumber())
                    propertyValue.setStyle("-fx-fill: blue");
                else
                    propertyValue.setStyle("-fx-fill: brown");

                textFlow.getChildren().addAll(propertyName, new Text(" : "), propertyValue);
            }

            if (iterator.hasNext())
                textFlow.getChildren().add(new Text(",\n"));

        }

        if (indention.equals(INITIAL_INDENTION))
            indention = "";

        Text endBraces = new Text("\n" + indention + "}");
        endBraces.setStyle("-fx-fill: blue;-fx-font-weight: bold");
        textFlow.getChildren().add(endBraces);
    }

    @FXML
    protected void copy() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        clipboard.setContent(Map.of(DataFormat.PLAIN_TEXT, output.toPrettyString()));
    }


}