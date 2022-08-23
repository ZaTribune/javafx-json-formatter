package com.zatribune.devtools;

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


    @FXML
    protected void beautifyJson(String input) {
        try {
            output = mapper.readValue(input, ObjectNode.class);
            textFlow.getChildren().clear();
            doSomeMagic(output, null, INITIAL_INDENTION, textFlow);
        } catch (JsonProcessingException ex) {
            handleJsonBoardError(ex,textArea,textFlow);
            //ex.printStackTrace();
        }
    }
    private void handleJsonBoardError(JsonProcessingException ex,TextArea jsonFormatInput,TextFlow textFlow){
        if (jsonFormatInput.getText().length()==0) {//no need to do anything
            textFlow.getChildren().clear();
            return;
        }
        int errorOffset = (int) ex.getLocation().getCharOffset()-1;
        char errorChar=' ';
        if (errorOffset>=0)
            errorChar=jsonFormatInput.getText().charAt(errorOffset);
        else//force reset to 0
            errorOffset=0;
        if (!jsonFormatInput.getText().startsWith("{")){//definitely not json
            Text invalidText=new Text(jsonFormatInput.getText());
            invalidText.setFill(Color.RED);
            textFlow.getChildren().setAll(invalidText);
            return;
        }
        if (errorChar=='\n'||errorChar==':'){
            errorOffset-=1;
            errorChar=jsonFormatInput.getText().charAt(errorOffset);
        }
        else if (Character.isLetterOrDigit(errorChar)&&jsonFormatInput.getText(0,errorOffset).lastIndexOf(":")>
                jsonFormatInput.getText(0,errorOffset).lastIndexOf(",")) {
            errorOffset=jsonFormatInput.getText(0,errorOffset).lastIndexOf(":");
            errorChar=jsonFormatInput.getText().charAt(errorOffset);
        }
        Text before = new Text(jsonFormatInput.getText(0, errorOffset));
        Text error = new Text(""+errorChar);
        error.setStyle("-fx-fill: red;-fx-font-size: large;-fx-font-weight: bold");

        //remove the code part -- we have 2 different ways to display it
        Tooltip.install(error,new Tooltip(ex.getOriginalMessage().replaceAll(", code .\\d", "")
                .replaceAll(" \\(code .*\\d\\)", "")));

        Text after=new Text(jsonFormatInput.getText(errorOffset+1,jsonFormatInput.getText().length()));
        textFlow.getChildren().clear();
        textFlow.getChildren().addAll(before,error,after);
        playAnimation(error);
    }
    private void playAnimation(Node error){
        FadeTransition ft = new FadeTransition(Duration.seconds(1), error);
        ft.setFromValue(1.0);
        ft.setToValue(0.1);
        ScaleTransition st=new ScaleTransition(Duration.seconds(1),error);
        st.setFromX(0.75);
        st.setToX(1.5);
        st.setFromY(0.75);
        st.setToY(1.5);
        ParallelTransition pt = new ParallelTransition(error, ft, st);
        pt.setCycleCount(Animation.INDEFINITE);
        pt.setAutoReverse(true);
        pt.play();
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