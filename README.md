# JAct

This is a simple implementation of an actor framework.  An actor is a computational unit
that sends and receives messages.  Actors only process one message a time which means
they can work in parallel like distributed threads.

Actors encapsulate behavior, they can change their behavior at runtime, they can maintain
state.  They have a mailbox which contains the messages being sent to them, from which
they pull messages first-in, first-out and process them.  They each have a name, which is
used to address the messages they send to each other.

# Usage

```java
ActorSystem system = new ActorSystem()
ActorRef<String, String> ref = system.register("hello", new Actor<String, String>() {
    @Override
    public String onMessage(String message) {
        return "received: " + message;
    }
});
Future<String> replyF = ref.ask("hello");
String reply = replyF.get();
assertEquals("received: hello", reply);
```

# Notes

This is not a production grade system.  Use Akka if you are looking for that.  This is
just a hobby project.