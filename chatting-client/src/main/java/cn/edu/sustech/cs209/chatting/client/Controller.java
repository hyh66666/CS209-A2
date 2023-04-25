package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {

    @FXML
    ListView<Message> chatContentList;
    @FXML
    Label currentUsername;
    @FXML
    Label currentOnlineCnt;
    @FXML
    ListView<String> chatList;
    @FXML
    TextArea inputArea;
    String username;
    Socket socket;
    OutputStream outputStream;
    InputStream inputStream;
    Scanner in;
    PrintWriter out;
    String response;
    String[] onlineClient;
    ObservableList<Message> messages = FXCollections.observableArrayList();

    ObservableList<String> privateChats_Fx = FXCollections.observableArrayList();
    // Map containing the username of each private chat and its corresponding window
    Map<String, List<Message>> privateChatWindows = new HashMap<>();
    Map<String, List<Message>> groupChatWindows = new HashMap<>();
    Map<String, List<String>> groupMap = new HashMap<>();
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
            username = input.get();
            int Port = 1234;
            try {
                socket = new Socket("localhost", Port);
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();
                in = new Scanner(inputStream);
                out = new PrintWriter(outputStream, true);
                out.println(username);
                while ((response = in.nextLine()) != null) {
                    System.out.println(response);
                    if (response.equals("Name already exists. Please enter another name.")){
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.titleProperty().set("Error");
                        alert.headerTextProperty().set(response);
                        alert.showAndWait();
                        dialog = new TextInputDialog();
                        dialog.setTitle("Login");
                        dialog.setHeaderText(null);
                        dialog.setContentText("Username:");
                        Optional<String> input1 = dialog.showAndWait();
                        if (input1.isPresent() && !input1.get().isEmpty()){
                            username = input1.get();
                            out.println(username);
                        }
                    } else {
                        break;
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }


        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }
        chatContentList.setCellFactory(new MessageCellFactory());
        currentUsername.setText("Current User: " + username);

//        onlineCountUpdater.scheduleAtFixedRate(this::updateLabels, 0, 5, TimeUnit.SECONDS);
        new Thread(() -> {
                while ((response = in.nextLine()) != null) {
                    if (response.startsWith("From")){
                        String[] pre = response.split(":");
                        String sender = pre[0].split(" ")[1];
                        String data = pre[1].substring(1);
                        Message message = new Message(sender, username, data);
                        privateChatWindows.get(sender).add(message);
                    }
                }
        }).start();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                Platform.runLater(() -> {
                    if (chatList.getSelectionModel().getSelectedItem()!=null){
                        messages.clear();
                        messages.setAll(privateChatWindows.get(chatList.getSelectionModel().getSelectedItem()));
                        chatContentList.setItems(messages);
                    }
                    String command = "getUsers null" + "\n" + "null";
                    out.println(command);
                    onlineClient = response.split(",");
                    currentOnlineCnt.setText("Online: " + onlineClient.length);
                });
            }
        }, 10, 50);
    }



    @FXML
    public void createPrivateChat() throws InterruptedException {
        AtomicReference<String> user = new AtomicReference<>();
        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out
        Thread.sleep(2*100);
        for (String item : onlineClient) {
            if (!item.equals(username)){
                userSel.getItems().add(item);
            }
        }

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
            if (privateChatWindows.containsKey(user.get())) {
                changePanel(user.get());
            } else {
                createPanel(user.get());
            }
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() throws InterruptedException {
        Stage stage = new Stage();
        // 从服务器获取所有用户的名字
        String command = "getUsers null" + "\n" + "null";
        out.println(command);
        Thread.sleep(2*100);
        String[] allUsers = onlineClient;
        List<CheckBox> checkBoxes = new ArrayList<>();
        for (String allUser : allUsers) {
            if (!allUser.equals(username)){
                checkBoxes.add(new CheckBox(allUser));

            }
        }
        Button confirmButton = new Button("OK");
        confirmButton.setOnAction(event -> {
            // 统计被选中的用户
            List<String> list = new ArrayList<>();
            list.add(username);
            StringBuilder sb = new StringBuilder();
            for (CheckBox checkBox : checkBoxes) {
                if (checkBox.isSelected()) {
                    list.add(checkBox.getText());
                }
            }
            Collections.sort(list);
            int count = list.size();
            if (count > 3) {
                for (int i = 0; i < list.size(); i++) {
                    if (i<3){
                        sb.append(list.get(i)).append(", ");
                    }
                    else {
                        sb.setLength(sb.length() - 2);
                        sb.append("... (").append(count).append(")");
                        break;
                    }
                }
            } else {
                for (String s : list) {
                    sb.append(s).append(", ");
                }
                sb.setLength(sb.length() - 2);
                sb.append(" (").append(count).append(")");
            }
            String title = sb.toString();
            groupMap.put(title,list);
            if (privateChatWindows.containsKey(title)) {
//                changePanel(title);
            } else {
                createPanel(title);
            }
            System.out.println(title);
            stage.close();
        });
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.getChildren().addAll(checkBoxes);

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().addAll(confirmButton);

        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.getChildren().addAll(root, buttonBox);
        // 创建场景
        Scene scene = new Scene(container, 300, 200);
        // 设置舞台

        stage.setTitle("创建群聊");
        stage.setScene(scene);
        stage.show();

    }


    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        String data = inputArea.getText();
        if (privateChatWindows.size() != 0 && !data.equals("")){
            inputArea.setText("");
            String recipient = chatList.getSelectionModel().getSelectedItem();
            String command = "private "+ recipient + "\n" + data;
            out.println(command);
            Message message = new Message(username, recipient, data);
            privateChatWindows.get(recipient).add(message);

        }
        // TODO
    }

    public void createPanel(String name) {
        privateChats_Fx.add(name);
        chatList.setItems(privateChats_Fx);
        List<Message> messageList = new ArrayList<>();
        privateChatWindows.put(name, messageList);
        chatList.getSelectionModel().select(name);
    }
    public void changePanel(String name) {
//        System.out.println("changePanel:"+name);
        chatList.getSelectionModel().select(name);
//        messages.clear();
//        for (int i = 0; i < privateChatWindows.get(name).size(); i++) {
//            System.out.println(privateChatWindows.get(name).get(i).getData());
//        }
//        messages.setAll(privateChatWindows.get(name));
//        chatContentList.setItems(messages);
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

    public void updateLabels() {
        currentUsername.setText("Current User: " + username);
        currentOnlineCnt.setText("Online: " + onlineClient.length);
    }
}
