import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;

class Message {
    private User sender;
    private List<User> recipients;
    private String content = "";
    private long timestamp;

    public Message(User sender, List<User> recipients, String content) {
        this.sender = sender;
        this.recipients = recipients;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(User sender, User recipients, String content) {
        this.sender = sender;
        this.recipients = new ArrayList<>();
        this.recipients.add(recipients);
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public User getSender() {
        return sender;
    }

    public List<User> getRecipients() {
        return recipients;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String toString() {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String formattedDate = sdf.format(date);
        return formattedDate + " [" + sender.getUsername() + "] :" + content;
    }
}

// User class representing a chat application user
class User implements IterableByUser{
    private final String username;

    private MessageMemento messageMemento = new MessageMemento("",System.currentTimeMillis());
    private ChatHistory chatHistory;

    public User(String username) {
        this.username = username;
        this.chatHistory = new ChatHistory();
    }

    public void sendMessage(List<User> recipients, String content, ChatServer server) {
        chatHistory.setLastSent(content);
        server.sendMessage(this, recipients, content);
        messageMemento = new MessageMemento(content, System.currentTimeMillis());
    }

    public void sendMessage(User recipients, String content, ChatServer server) {
        chatHistory.setLastSent(content);
        server.sendMessage(this,recipients, content);
        messageMemento = new MessageMemento(content, System.currentTimeMillis());
    }

    public void receiveMessage(Message message) {
        chatHistory.addMessage(message);
        System.out.println(String.format("[%s] Received message from %s: %s", username, message.getSender().getUsername(), message.getContent()));
    }

    public void setMessageMemento(Message message)
    {
        this.messageMemento.setContent(message.getContent());
        this.messageMemento.setTimestamp(message.getTimestamp());
    }
    public void undoLastMessage() {
        if (this.messageMemento!= null) {
            System.out.println(String.format("[%s] Undid last message: %s", username, this.messageMemento.getContent()));
            messageMemento.setContent("");
            messageMemento.setTimestamp(0);
        } else {
            System.out.println("[%s] No messages to undo!" +  username);
        }
    }

    public ChatHistory getHistory() {
         return chatHistory;
    }

    public void getChatHistory(User user) {
        chatHistory.getHistory(user);
    }
    public void getChatHistory() {
        chatHistory.getHistory();
    }

    public String getUsername() {
        return username;
    }

    @Override
    public SearchMessagesByUser iterator(User userToSearchWith) {
        return chatHistory.iterator(userToSearchWith);
    }
}

// ChatHistory class to store chat history for a user
class ChatHistory implements IterableByUser{
    private final List<Message> messages;
    private String lastSent;

    public ChatHistory() {
        this.messages = new ArrayList<>();
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    public void removeMessage(User user) {
        for(Message message : this.messages)
        {
            if(message.getSender().getUsername() == user.getUsername())
            {
                messages.remove(message);
                return;
            }
        }
    }

    public void getHistory(User user) {
        for(Message message : this.messages)
        {
            if(message.getSender().getUsername() == user.getUsername())
            {
                System.out.println(message);
            }
        }

    }
    public void getHistory() {
        for(Message message : this.messages)
        {
            System.out.println(message);
        }

    }

    public void setLastSent(String lastSent) {
        this.lastSent = lastSent;
    }

    public String getLastSent() {
        return lastSent;
    }

    @Override
    public SearchMessagesByUser iterator(User userToSearchWith) {
        return new SearchMessagesByUser(messages, userToSearchWith);
    }
}

// MessageMemento class representing a snapshot of a message
class MessageMemento {
    private String content;

    private long timestamp;

    public MessageMemento(String content, long timestamp) {
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }

}

// ChatServer class acting as the mediator for communication
class ChatServer {
    private final Map<String, User> users;
    private final Map<String, List<String>> blockedUsers;

    public ChatServer() {
        this.users = new HashMap<>();
        this.blockedUsers = new HashMap<>();
    }

    public void registerUser(User user) {
        users.put(user.getUsername(), user);
    }

    public void unregisterUser(User user) {
        users.remove(user.getUsername());
        blockedUsers.remove(user.getUsername()); // Remove user from blocked lists as well
    }

    public void sendMessage(User sender, List<User> recipients, String content) {
        Message message = new Message(sender, recipients, content);
        sender.getHistory().setLastSent(content);
        sender.setMessageMemento(message);
        sender.getHistory().addMessage(message);
        recipients.stream()
                .filter(user -> !sender.getUsername().equals(user.getUsername()))
                .filter(user -> !isBlocked(sender.getUsername(), user.getUsername())) // Filter blocked users
                .forEach(user -> user.receiveMessage(message));
    }
    public void sendMessage(User sender, User recipients, String content) {
        Message message = new Message(sender, recipients, content);
        sender.getHistory().setLastSent(content);
        sender.setMessageMemento(message);
        sender.getHistory().addMessage(message);
        if(!isBlocked(sender.getUsername(), recipients.getUsername()))
        {
            recipients.receiveMessage(message);
        }
    }

    public void undoMessage(User sender, List<User> recipients) {
        sender.undoLastMessage();
        for(User user : recipients)
        {
            user.getHistory().removeMessage(sender);
        }

    }
    public void undoMessage(User sender, User recipient) {
        sender.undoLastMessage();
        recipient.getHistory().removeMessage(sender);

    }

    public void blockMessages(User blocker, User blocked) {
        List<String> blockedList = blockedUsers.getOrDefault(blocker.getUsername(), new ArrayList<>());
        blockedList.add(blocked.getUsername());
        blockedUsers.put(blocker.getUsername(), blockedList);
    }

    private boolean isBlocked(String sender, String recipient) {
        List<String> blockedList = blockedUsers.get(recipient);
        return blockedList != null && blockedList.contains(sender);
    }
}

interface IterableByUser {
    Iterator iterator(User userToSearchWith);
}
class SearchMessagesByUser implements Iterator<Message> {
    private final List<Message> messages;
    private final User userToSearch;
    private int currentIndex = 0;

    public SearchMessagesByUser(List<Message> messages, User userToSearch) {
        this.messages = messages;
        this.userToSearch = userToSearch;
    }

    @Override
    public boolean hasNext() {
        while (currentIndex < messages.size()) {
            Message message = messages.get(currentIndex);
            if (message.getSender().getUsername().equals(userToSearch.getUsername()) ||
                    message.getRecipients().stream().anyMatch(user -> user.getUsername().equals(userToSearch.getUsername()))) {
                return true;
            }
            currentIndex++;
        }
        return false;
    }

    @Override
    public Message next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return messages.get(currentIndex++);
    }
}
// Driver class to demonstrate the chat application functionality
public class ChatApp {

    public static void main(String[] args) {
        ChatServer server = new ChatServer();

        User alice = new User("Alice");
        User bob = new User("Bob");
        User charlie = new User("Charlie");

        server.registerUser(alice);
        server.registerUser(bob);
        server.registerUser(charlie);

        // Send messages to one or more Users through the chat server
        server.sendMessage(alice,Arrays.asList(bob, charlie), "Hello everyone!");
        server.sendMessage(bob, Arrays.asList(alice), "Hi Alice!");
        server.sendMessage(charlie,bob, "Hey Bob!");
        server.sendMessage(charlie,Arrays.asList(bob, alice), "Hi Alice and Bob!");
        server.sendMessage(charlie,alice, "How are you Alice?");


        System.out.println("\nBob's chat history of Alice before Alice undo:");
        bob.getChatHistory(alice);
        System.out.println();

        // Undo last Message alice sent to the other 2 users
        server.undoMessage(alice,Arrays.asList(bob, charlie));


        // Block messages, alice blocked message from Bob
        server.blockMessages(alice, bob);
        System.out.println();

        // Send another message (Bob's message will be blocked for Alice)
        server.sendMessage(bob,Arrays.asList(alice, charlie), "How's it going?");


        //view chat history from specific user
        System.out.println("\nAlice's chat history of Bob:");
        alice.getChatHistory(bob);

        System.out.println("\nBob's chat history of Alice:");
        bob.getChatHistory(alice);

        System.out.println("\nCharlie's chat history of All:");
        charlie.getChatHistory();

        //Iterator through charlie message to find message that involve bob
        System.out.println("\nIterating through charlie message to find message that involve Bob:");
        SearchMessagesByUser searchMessagesByUser = charlie.iterator(bob);
        while(searchMessagesByUser.hasNext())
        {
            System.out.println(searchMessagesByUser.next());
        }


    }
}