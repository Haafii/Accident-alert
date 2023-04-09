// Basic demo for accelerometer readings from Adafruit MPU6050

// ESP32 Guide: https://RandomNerdTutorials.com/esp32-mpu-6050-accelerometer-gyroscope-arduino/
// ESP8266 Guide: https://RandomNerdTutorials.com/esp8266-nodemcu-mpu-6050-accelerometer-gyroscope-arduino/
// Arduino Guide: https://RandomNerdTutorials.com/arduino-mpu-6050-accelerometer-gyroscope/

#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Wire.h>
#include <Arduino.h>
#include "HX711.h"
#include "soc/rtc.h"
#include <pthread.h>
#include <WiFi.h>
#include <ESPAsyncWebServer.h>


// constants for accident condition
#define gyro_x_max 50
#define gyro_y_max 50
#define gyro_z_max 50
#define acc_x_max 0
#define weight_max 0
#define distance_max 20

// indicate whether accident occured or not
bool ACCIDENT=false;
// global object to store MPU sensor data
sensors_event_t ACCEL, GYRO, temp;




#define TRIG_PIN_1 5
#define ECHO_PIN_1 18
#define TRIG_PIN_2 14
#define ECHO_PIN_2 27
Adafruit_MPU6050 mpu;
float duration_us_1, distance_cm_1, duration_us_2, distance_cm_2;
const int LOADCELL_DOUT_PIN = 16;
const int LOADCELL_SCK_PIN = 4;
HX711 scale;





// code for websocket server 

IPAddress local_ip(192, 168, 1, 1);
IPAddress gateway(192, 168, 1, 1);
IPAddress subnet(255, 255, 255, 0);
 
const char* ssid = "TrikkuEmergencyAlert";
const char* password =  "12345678";
 
AsyncWebServer server(80);
AsyncWebSocket ws("/ws");
AsyncWebSocketClient * CLIENT=NULL;
 
void WebsocketEvent(AsyncWebSocket * server, AsyncWebSocketClient * client, AwsEventType type, void * arg, uint8_t *data, size_t len){
    if(type == WS_EVT_CONNECT){
        Serial.println("Websocket client connection received");
        client->text("{\"status\":\"connected\"}");
        CLIENT=client;
    } 
    else if(type == WS_EVT_DISCONNECT){
        Serial.println("Client disconnected");
        CLIENT=NULL;
    }
}

void AlertClient(){
    if(CLIENT)
        CLIENT->text("{\"status\":\"accident\"}");
    else Serial.println("No clients connected to report accident");
}
 

 
// Accident detecting functions


int mpu_gyro(float gyro_x, float gyro_y, float gyro_z){
  int flag1 = 0;
  if(gyro_x > gyro_x_max){
    //cout<<"\nAccident";
    flag1 = 1;
  }
  if(gyro_y > gyro_y_max){
    //cout<<"\nAccident";
    flag1 = 1;
  }
  if(gyro_z > gyro_z_max){
    //cout<<"\nAccident";
    flag1 = 1;
  }
  return flag1;
}

float prev_acc_x=0;

int mpu_acc(float acc_x){
  float acc_diff_x;
  int flag2 = 0;
  acc_diff_x = acc_x - prev_acc_x;
  if(acc_diff_x < acc_x_max){
    //cout<<"\naccident";
    flag2 = 1;
  }
  prev_acc_x = acc_x;
  return flag2;
}

float prev_weight=0;

int load_cell(float weight){
  float weight_diff;
  int flag3 = 0;
  weight_diff = weight - prev_weight;
  if(weight_diff < weight_max){
    //cout<<"\naccident";
    flag3 = 1;
  }
  prev_weight = weight;
  return flag3;
}

int distance(float dis){
  int flag4 = 0;
  if(dis < distance_max){
    //cout<<"\naccident";
    flag4 = 1;
  }
  return flag4;
}


void* checkAccident(){
    int condition1,condition2,condition3,condition4,condition5;
    while(!ACCIDENT){
        condition1 = mpu_gyro(GYRO.gyro.x,GYRO.gyro.x,GYRO.gyro.x);
        condition2 = mpu_acc(ACCEL.acceleration.x);
        condition3 = load_cell(54);
        condition4 = distance(49);
        condition5 = distance(50);
        if( condition1 == 1 && condition2 == 1 && condition3 == 1){
            if(condition4 == 1 || condition5 == 1){
                ACCIDENT=true;
                return 1;
            }
        }
        delay(1000);
    }

    // code for Contacting websocket server
    AlertClient();
}





void setup(void) {


  Serial.begin(115200);
  while (!Serial)
    delay(10);  // will pause Zero, Leonardo, etc until serial console opens


// websocket setup
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
        delay(1000);
        Serial.println("Connecting to WiFi..");
    }


    Serial.println(WiFi.localIP());
    ws.onEvent(WebsocketEvent);
    server.addHandler(&ws);
    server.begin();


  //mpu sensor
  Serial.println("Adafruit MPU6050 test!");

  // Try to initialize!
  if (!mpu.begin()) {
    Serial.println("Failed to find MPU6050 chip");
    while (1) {
      delay(10);
    }
  }
  Serial.println("MPU6050 Found!");

  mpu.setAccelerometerRange(MPU6050_RANGE_8_G);
  Serial.print("Accelerometer range set to: ");
  switch (mpu.getAccelerometerRange()) {
    case MPU6050_RANGE_2_G:
      Serial.println("+-2G");
      break;
    case MPU6050_RANGE_4_G:
      Serial.println("+-4G");
      break;
    case MPU6050_RANGE_8_G:
      Serial.println("+-8G");
      break;
    case MPU6050_RANGE_16_G:
      Serial.println("+-16G");
      break;
  }
  mpu.setGyroRange(MPU6050_RANGE_500_DEG);
  Serial.print("Gyro range set to: ");
  switch (mpu.getGyroRange()) {
    case MPU6050_RANGE_250_DEG:
      Serial.println("+- 250 deg/s");
      break;
    case MPU6050_RANGE_500_DEG:
      Serial.println("+- 500 deg/s");
      break;
    case MPU6050_RANGE_1000_DEG:
      Serial.println("+- 1000 deg/s");
      break;
    case MPU6050_RANGE_2000_DEG:
      Serial.println("+- 2000 deg/s");
      break;
  }
  mpu.setFilterBandwidth(MPU6050_BAND_5_HZ);
  Serial.print("Filter bandwidth set to: ");
  switch (mpu.getFilterBandwidth()) {
    case MPU6050_BAND_260_HZ:
      Serial.println("260 Hz");
      break;
    case MPU6050_BAND_184_HZ:
      Serial.println("184 Hz");
      break;
    case MPU6050_BAND_94_HZ:
      Serial.println("94 Hz");
      break;
    case MPU6050_BAND_44_HZ:
      Serial.println("44 Hz");
      break;
    case MPU6050_BAND_21_HZ:
      Serial.println("21 Hz");
      break;
    case MPU6050_BAND_10_HZ:
      Serial.println("10 Hz");
      break;
    case MPU6050_BAND_5_HZ:
      Serial.println("5 Hz");
      break;
  }
  //ultrasonic pins
  pinMode(TRIG_PIN_1, OUTPUT);
  pinMode(ECHO_PIN_1, INPUT);
  pinMode(TRIG_PIN_2, OUTPUT);
  pinMode(ECHO_PIN_2, INPUT);
  

  //load cell
  rtc_clk_cpu_freq_set(RTC_CPU_FREQ_80M);
  Serial.println("HX711 Demo");

  Serial.println("Initializing the scale");

  scale.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);

  Serial.println("Before setting up the scale:");
  Serial.print("read: \t\t");
  Serial.println(scale.read());      // print a raw reading from the ADC

  Serial.print("read average: \t\t");
  Serial.println(scale.read_average(20));   // print the average of 20 readings from the ADC

  Serial.print("get value: \t\t");
  Serial.println(scale.get_value(5));   // print the average of 5 readings from the ADC minus the tare weight (not set yet)

  Serial.print("get units: \t\t");
  Serial.println(scale.get_units(5), 1);  // print the average of 5 readings from the ADC minus tare weight (not set) divided
            // by the SCALE parameter (not set yet)
            
  scale.set_scale(-471.497);
  //scale.set_scale(-471.497);                      // this value is obtained by calibrating the scale with known weights; see the README for details
  scale.tare();               // reset the scale to 0

  Serial.println("After setting up the scale:");

  Serial.print("read: \t\t");
  Serial.println(scale.read());                 // print a raw reading from the ADC

  Serial.print("read average: \t\t");
  Serial.println(scale.read_average(20));       // print the average of 20 readings from the ADC

  Serial.print("get value: \t\t");
  Serial.println(scale.get_value(5));   // print the average of 5 readings from the ADC minus the tare weight, set with tare()

  Serial.print("get units: \t\t");
  Serial.println(scale.get_units(5), 1);        // print the average of 5 readings from the ADC minus tare weight, divided
            // by the SCALE parameter set with set_scale
  Serial.println("Readings:");
  
  Serial.println("");

    // Thread creation
    pthread_t thread1,thread2,thread3,thread4;

    pthread_create(&thread1, NULL,ultrasonic, NULL);
    pthread_create(&thread2, NULL,mpu_6050, NULL);
    pthread_create(&thread3, NULL,loadCell, NULL);
    pthread_create(&thread4, NULL,checkAccident, NULL);

}

void loop(){}

void* ultrasonic() {

    while(!ACCIDENT){
        digitalWrite(TRIG_PIN_1, HIGH);
        delayMicroseconds(5);
        digitalWrite(TRIG_PIN_1, LOW);
        duration_us_1 = pulseIn(ECHO_PIN_1, HIGH);
        delayMicroseconds(2);
        digitalWrite(TRIG_PIN_2, HIGH);
        delayMicroseconds(5);
        digitalWrite(TRIG_PIN_2, LOW);
        duration_us_2 = pulseIn(ECHO_PIN_2, HIGH);
        distance_cm_1 = 0.017 * duration_us_1;
        distance_cm_2 = 0.017 * duration_us_2;
        Serial.print("distance: ");
        Serial.print(distance_cm_1);
        Serial.print(" cm");
        Serial.print("               distance: ");
        Serial.print(distance_cm_2);
        Serial.println(" cm");
    }
}
void* mpu_6050() {

    while(!ACCIDENT){
        mpu.getEvent(&ACCEL, &GYRO, &temp);
        /* Print out the values */
        Serial.print("Acceleration X: ");
        Serial.print(ACCEL.acceleration.x);
        Serial.print(", Y: ");
        Serial.print(ACCEL.acceleration.y);
        Serial.print(", Z: ");
        Serial.print(ACCEL.acceleration.z);
        Serial.println(" m/s^2");

        Serial.print("Rotation X: ");
        Serial.print(GYRO.gyro.x);
        Serial.print(", Y: ");
        Serial.print(GYRO.gyro.y);
        Serial.print(", Z: ");
        Serial.print(GYRO.gyro.z);
        Serial.println(" rad/s");
    }
}
void* loadCell(){
    while(!ACCIDENT){
        Serial.print("one reading:\t");
        Serial.print(scale.get_units(), 1);
        Serial.print("\t| average:\t");
        Serial.println(scale.get_units(10), 5);

        scale.power_down();             // put the ADC in sleep mode
        delay(5000);
        scale.power_up(); 
    }
}