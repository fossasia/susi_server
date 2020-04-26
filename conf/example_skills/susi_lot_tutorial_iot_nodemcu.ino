
#include <WiFiServerSecure.h>
#include <WiFiClientSecure.h>
#include <WiFiClientSecureBearSSL.h>
#include <ESP8266WiFi.h>
#include <ESP8266WiFiMulti.h>
#include <WiFiUdp.h>
#include <ESP8266WiFiType.h>
#include <CertStoreBearSSL.h>
#include <ESP8266WiFiAP.h>
#include <WiFiClient.h>
#include <BearSSLHelpers.h>
#include <WiFiServer.h>
#include <ESP8266WiFiScan.h>
#include <WiFiServerSecureBearSSL.h>
#include <ESP8266WiFiGeneric.h>
#include <ESP8266WiFiSTA.h>
#include <WiFiClientSecureAxTLS.h>
#include <WiFiServerSecureAxTLS.h>
#include "SSD1306Wire.h" // legacy include: `#include "SSD1306.h"`

const char* ssid = "barbieland";
const char* password = "s3cr3tp455w0rd";
WiFiServer server(80);
int ports[] = {D0, D1, D2, D3, D4, D5, D6, D7, D8};
int pmode[] = {0, -1, -1, 0, 0, 1, 1, 1, 0}; // 1 = in, 0 = out, -1 = undefined
SSD1306Wire display(0x3c, D1, D2);
 
// D1 Mini Exceptions:
// D3 + D8 cannot be use for input, no connection possible during flashing
// D4 is blue LED (on when D4 is LOW) and cannot be GND during flashing (so better not use it as input)

// Test URLs
// http://192.168.1.51/
// http://192.168.1.51/d0=0,d1=0,d2=0,d3=0,d4=0,d5=0,d6=0,d7=0,d8=0
// http://192.168.1.51/d0=1,d1=1,d2=1,d3=1,d4=1,d5=1,d6=1,d7=1,d8=1

void setup() {
  Serial.begin(74880);
  delay(10);
  
  display.init();
  display.clear();
  display.flipScreenVertically();
  display.setContrast(255);
  display.setColor(WHITE);
  display.setFont(ArialMT_Plain_10);
  display.drawString(0, 0, "Hello world 1");
  display.drawString(10, 0, "Hello world 2");
  display.display();
  
  // development checksSerial.println
  Serial.println("check pin d0-d8, a0:");
  Serial.println("D1 Mini: 16, 5, 4, 0, 2, 14, 12, 13, 15, 17");
  Serial.print("CHECK  : ");
  for (int i = 0; i <= 8; i++) { Serial.print(ports[i]); Serial.print(", "); } Serial.println(A0);
  Serial.print("MODE   : ");
  for (int i = 0; i <= 8; i++) { Serial.print(pmode[i]); Serial.print(", "); } Serial.println("1");

  // initialize mode and set output pins to LOW
  pinMode(A0, INPUT);
  for (int i = 0; i <= 8; i++) {
    if (pmode[i] == 0) {
      pinMode(ports[i], OUTPUT); digitalWrite(ports[i], LOW);
    } else if (pmode[i] == 1) {
      pinMode(ports[i], INPUT);
    }
  }
  
  Serial.print("Connecting to "); Serial.println(ssid);
  IPAddress ip(192, 168, 1, 51);
  IPAddress gateway(192, 168, 1, 3);
  IPAddress subnet(255, 255, 255, 0);
  WiFi.config(ip, gateway, subnet);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("WiFi connected"); 
  server.begin();
  Serial.print("Server started, connect url: http://");
  Serial.println(WiFi.localIP());
}

void loop() {
  // get connection
  WiFiClient client = server.available();
  if (!client) { return; }
  while (!client.available()) { delay(1); }

  // read request
  String request = client.readStringUntil('\r');
  Serial.println(request);
  client.flush();

  if (request.indexOf("favicon") != -1) {
    client.print("HTTP/1.1 410 Gone\r\n\r\n"); // browser request
  } else {
    // very lazy parameter parsing
    for (int i = 0; i <= 8; i++) {
      String off = "d"; off = off + i + "=0";
      String onn = "d"; onn = onn + i + "=1";
      if (pmode[i] == 0 && request.indexOf(off) != -1) digitalWrite(ports[i], LOW);
      if (pmode[i] == 0 && request.indexOf(onn) != -1) digitalWrite(ports[i], HIGH);
    }

    // create JSON response
    // must be done before response header writing to determine body length
    String r = "{";
    for (int i = 0; i <= 8; i++) {
      r = r + "\"d" + i + "\":" + digitalRead(ports[i]) + ";";
    }
    r = r + "\"a0\":" + analogRead(A0) + "}\r\n"; // CRLF is not really needed here but maybe it helps some JSON parsers
    Serial.print("OUT "); Serial.print(r); // no println here because r ends with CRLF
  
    // write response head
    client.print("HTTP/1.1 200 OK\r\n");
    client.print("Content-Type: application/json\r\n");
    client.print("Connection: close\r\n"); // clients should not try to keep-alive connections
    client.print("Content-Length: "); client.print(r.length()); // must be there to be able to quickly close connection
    client.print("\r\n\r\n"); // End Of Header
  
    // write response body
    client.print(r);
  }
  client.flush();
  client.stop();
}
