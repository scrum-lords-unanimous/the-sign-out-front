public class Queue {

    Node front;

    Queue(String contents){
        front = new Node(contents);
    }

    /**
     * Adds new node to end of the line
     * @param contents
     */
    void append(String contents){
        // check is front node is occupied
        if (front == null){
            // creates new node for front
            front = new Node(contents);
        }
        else{
            // puts the current node at the front
            Node currentNode = front;
            // while next node is occupied moves to next node, stops loop once next node is empty
            while (currentNode.next != null) {
                currentNode = currentNode.next;
            }
            // places current node in next spot
            currentNode.next = new Node(contents);
        }
    }

    /**
     * If front node is not null, removes data from node and moves the next node to the front
     * @return line with front node removed if there are nodes in line
     */
    public String remove(){
        // checks if front is null
        if (front != null){
            // removes data from node
            String removeData = front.data;
            // moves next node to front
            front = front.next;
            return removeData;
        }else{
            return null;
        }
    }

    /**
     * Converts node data to string
     * @return node as a string
     */
    @Override
    public String toString(){
        // create string builder
        StringBuilder returnString = new StringBuilder();
        // set current node to front
        Node currentNode = front;
        // loops while current node is not null
        while (currentNode != null){
            // convert node data to string and adds to string
            returnString.append(currentNode.data + " -> ");
            // moves to next node
            currentNode = currentNode.next;
        }
        // adds null maker to end of string
        returnString.append("null");
        // returns string
        return returnString.toString();
    }
}

class Node {
    String data;
    Node next = null;

    Node (String contents){
        data = contents;
    }
}