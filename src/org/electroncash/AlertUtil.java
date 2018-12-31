package org.electroncash;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;

public class AlertUtil
{
  private Display display;
  private boolean debugEnabled;
  
  public AlertUtil(Display display, boolean debugEnable)
  {
    this.display = display;
  }
  
  public void sendErrorAlert(Displayable displayable, String message) {
    if (debugEnabled) {
      System.out.println("Error message " + message);
    }
    else {
      Alert alert = new Alert("Error", message, null, AlertType.ERROR);
      alert.setTimeout(5000);
      display.setCurrent(alert, displayable);
    }
  }
  
  public void sendErrorAlert(String message) {
    if (debugEnabled) {
      System.out.println("Error message " + message);
    }
    else {
      Alert alert = new Alert("Error Message", message, null, AlertType.ERROR);
      alert.setTimeout(-2);
      display.setCurrent(alert);
    }
  }
  
  public void sendInfoAlert(String message) {
    if (debugEnabled) {
      System.out.println("Error message " + message);
    }
    else {
      Alert alert = new Alert("Info", message, null,AlertType.INFO);
      alert.setTimeout(5000);
      display.setCurrent(alert);
    }
  }
}
