package com.nicolai.alarm_clock.util;

public class DaysUtil {

    public static int nextDay(String weekDay, String xFormat){

        //Position des ersten Buchstaben des Heutigen tages in der 2*x codierung der Weckertage
        int todayTimes2 = intForDay(weekDay) * 2;

        //Wenn am ende immer noch -> Wecker hat keinen Tag ausgewählt
        int nearest = 100;

        //Ist kein Tag markiert
        if(xFormat.equals("xxxxxxxxxxxxxx")){
            return 20;

            //Ist es heute
        }else if(xFormat.charAt(todayTimes2) != 'x'){
            nearest = todayTimes2;

        }else{
            //Abstand nach hinten zum aktuellen Tag
            int nearestDist = 100;
            for(int i = 0; i < 13; i = i+2){
                //ist der Tag ausgewählt
                if(xFormat.charAt(i) != 'x'){
                    //Wenn der erste Tag, der gefunden wurde
                    if(nearest == 100){
                        nearest = i;

                        int distance = 0;
                        int todayCopy = todayTimes2;

                        //Symulate the circle with 7 steps 0-12
                        while(todayCopy != nearest){
                            todayCopy = todayCopy + 2;
                            distance++;

                            if(todayCopy > 12){
                                todayCopy = 0;
                            }
                        }

                        nearestDist = distance;

                    }else {

                        int distance = 0;
                        int todayCopy = todayTimes2;

                        //Symulate the circle with 7 steps 0-12
                        while(todayCopy != i){
                            todayCopy = todayCopy + 2;
                            distance++;

                            if(todayCopy > 12){
                                todayCopy = 0;
                            }
                        }

                        //Wenn die Entfernung zum aktuellen Tag kürzer als die letzte ist nearest erneuern
                        if(nearestDist > distance){
                            nearestDist = distance;
                            nearest = i;
                        }
                    }
                }
            }
        }

        return nearest / 2;
    }

    public static int intForDay(String day){
        //Standart Montag
        int dayInInt = 0;

        switch (day){
            case "Dienstag": dayInInt = 1; break;
            case "Mittwoch": dayInInt = 2; break;
            case "Donnerstag": dayInInt = 3; break;
            case "Freitag": dayInInt = 4; break;
            case "Samstag": dayInInt = 5; break;
            case "Sonntag": dayInInt = 6; break;
        }

        return dayInInt;
    }
}
