import java.io.*;
import java.util.Scanner;

// 1. class to hold one bill / customer record 
class Bill {
    String id;
    String name;
    double amount;
    double gst;
    double total;
    String status;      // "Waiting" or "Paid"

    Bill(String id, String name, double amount, String status) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.status = status;
        this.gst = amount * 0.05;          // GST 5%
        this.total = amount + this.gst;
    }
}

public class CustomerBillingSystem {

    static final int MAX = 100;
    static final String FILE_NAME = "bills.txt";

    //  2. Array-based circular queue (FIFO) for WAITING customers 
    static Bill[] queue = new Bill[MAX];
    static int front = 0;       // index of the first customer
    static int size = 0;        // how many customers are waiting

    //  3. Array that stores ALL bills (waiting + paid) for searching 
    static Bill[] allBills = new Bill[MAX * 5];
    static int totalBills = 0;

    static int nextNumber = 101;    // first bill ID is BL101

    static Scanner sc = new Scanner(System.in);

    // = QUEUE METHODS =
    static boolean isQueueEmpty() {
        return size == 0;
    }

    static boolean isQueueFull() {
        return size == MAX;
    }

    static void enqueue(Bill b) {
        int rear = (front + size) % MAX;    // next free place at the back
        queue[rear] = b;
        size++;
    }

    static Bill dequeue() {
        Bill b = queue[front];
        queue[front] = null;
        front = (front + 1) % MAX;
        size--;
        return b;
    }

    // = HELPER METHODS =
    // prints 1000.0 as "1000" and 52.5 as "52.5"
    static String fmt(double n) {
        if (n == (long) n) {
            return String.valueOf((long) n);
        }
        return String.valueOf(n);
    }

    // FEATURE 1: ADD CUSTOMER
    static void addCustomer() {
        if (isQueueFull() || totalBills >= allBills.length) {
            System.out.println("Queue is full. Cannot add more customers.");
            return;
        }

        System.out.print("Customer: ");
        String name = sc.nextLine().trim();
        if (name.equals("")) {
            System.out.println("Invalid name. Name cannot be empty.");
            return;
        }

        System.out.print("Amount: ");
        double amount;
        try {
            amount = Double.parseDouble(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount. Amount must be greater than 0.");
            return;
        }

        if (amount <= 0) {
            System.out.println("Invalid amount. Amount must be greater than 0.");
            return;
        }

        String id = "BL" + nextNumber;
        nextNumber++;

        Bill b = new Bill(id, name, amount, "Waiting");
        enqueue(b);                      // add to waiting queue
        allBills[totalBills++] = b;      // also keep in the all-bills list

        System.out.println("Customer added. ID: " + id + " | " + name
                + " | Amount: " + fmt(amount) + " | Position: " + size);
    }

    // FEATURE 2: PROCESS NEXT BILL 
    static void processNextBill() {
        if (isQueueEmpty()) {
            System.out.println("Queue is empty. No bill to process.");
            return;
        }

        Bill b = dequeue();              // FIFO: first in, first out
        b.status = "Paid";

        System.out.println("Bill generated. " + b.id + " | " + b.name
                + " | Amount: " + fmt(b.amount));
        System.out.println("GST 5%: " + fmt(b.gst) + " | Total: " + fmt(b.total));

        if (isQueueEmpty()) {
            System.out.println("Queue is now empty.");
        }
    }

    // FEATURE 3: VIEW QUEUE 
    static void viewQueue() {
        if (isQueueEmpty()) {
            System.out.println("Queue is empty.");
            return;
        }

        System.out.println("--- Waiting Queue ---");
        for (int i = 0; i < size; i++) {
            Bill b = queue[(front + i) % MAX];
            System.out.println((i + 1) + ". " + b.id + " | " + b.name
                    + " | Amount: " + fmt(b.amount));
        }
    }

    // FEATURE 4: SEARCH BILL BY ID (Linear Search) 
    static int searchBill(String id) {
        for (int i = 0; i < totalBills; i++) {
            if (allBills[i].id.equalsIgnoreCase(id)) {
                return i;               // found: return position
            }
        }
        return -1;                      // not found
    }

    static void searchMenu() {
        System.out.print("Enter Bill ID: ");
        String id = sc.nextLine().trim();

        int index = searchBill(id);
        if (index == -1) {
            System.out.println("Bill not found.");
            return;
        }

        Bill b = allBills[index];
        System.out.println("Found: " + b.id + " | " + b.name
                + " | Total: " + fmt(b.total) + " | Status: " + b.status);
    }

    //  FILE HANDLING
    // File format (one bill per line):  id,name,amount,status
    static void saveToFile() {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME));
            for (int i = 0; i < totalBills; i++) {
                Bill b = allBills[i];
                pw.println(b.id + "," + b.name + "," + b.amount + "," + b.status);
            }
            pw.close();
            System.out.println("Data saved to " + FILE_NAME);
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    static void loadFromFile() {
        File f = new File(FILE_NAME);
        if (!f.exists()) {
            return;                      // first run: nothing to load
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 4) {
                    continue;            // skip bad lines
                }

                String id = p[0];
                String name = p[1];
                double amount = Double.parseDouble(p[2]);
                String status = p[3];

                Bill b = new Bill(id, name, amount, status);
                allBills[totalBills++] = b;

                if (status.equals("Waiting")) {
                    enqueue(b);          // rebuild queue in the same order
                }

                // keep ID counter ahead of the largest loaded ID
                int num = Integer.parseInt(id.substring(2));
                if (num >= nextNumber) {
                    nextNumber = num + 1;
                }
            }
            br.close();
            System.out.println("Loaded " + totalBills + " record(s) from " + FILE_NAME);
        } catch (Exception e) {
            System.out.println("Error loading file: " + e.getMessage());
        }
    }

    //  MAIN---
    public static void main(String[] args) {
        loadFromFile();

        int choice = 0;
        while (choice != 5) {
            System.out.println();
            System.out.println("===== CUSTOMER BILLING SYSTEM =====");
            System.out.println("1 - Add Customer");
            System.out.println("2 - Process Next Bill");
            System.out.println("3 - View Queue");
            System.out.println("4 - Search Bill by ID");
            System.out.println("5 - Exit (Save)");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                choice = 0;
            }

            switch (choice) {
                case 1: addCustomer();      break;
                case 2: processNextBill();  break;
                case 3: viewQueue();        break;
                case 4: searchMenu();       break;
                case 5: saveToFile();       break;
                default: System.out.println("Invalid choice. Enter 1-5.");
            }
        }
        System.out.println("Thank you!");
    }
}
