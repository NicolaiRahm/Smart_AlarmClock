package com.nicolai.alarm_clock.room_database;

import com.nicolai.alarm_clock.pojos.WeckerPOJO;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface WeckerDao {

    //Find specific value with given id
    @Query("SELECT on_off FROM wecker WHERE id = :id ")
    boolean findOnOffById(long id);
    @Query("SELECT shared FROM wecker WHERE id = :id ")
    char findSharedById(long id);
    @Query("SELECT sender_id FROM wecker WHERE id = :id ")
    String findSenderIdById(long id);
    @Query("SELECT sender_wecker_id FROM wecker WHERE id = :id ")
    String findSenderWeckerIdById(long id);
    @Query("SELECT von_xy FROM wecker WHERE id = :id ")
    String findVonXYById(long id);

    //Für automatischen Weckerdownload
    @Query("SELECT * FROM wecker WHERE (sender_id = :sender_id AND sender_wecker_id = :senderWecker_id) ")
    WeckerPOJO bySender_id(String sender_id, String senderWecker_id);

    //Liste aller ids --> find id by position
    @Query("SELECT id FROM wecker")
    List<Long> idList();
    @Query("SELECT days FROM wecker")
    List<String> tageList();
    @Query("SELECT on_off FROM wecker")
    List<Boolean> onOffList();
    @Query("SELECT hour FROM wecker")
    List<Integer> hourList();
    @Query("SELECT minute FROM wecker")
    List<Integer> minuteList();
    @Query("SELECT shared FROM wecker")
    List<Character> sharedList();
    @Query("SELECT sender_id FROM wecker")
    List<String> senderIdList();
    @Query("SELECT sender_wecker_id FROM wecker")
    List<String> senderWeckerIdList();

    //Ganzer Wecker by id
    @Query("SELECT * FROM wecker WHERE id = :id ")
    WeckerPOJO findRowByID(long id);

    //TODO dataPaging plus für verwalten und alarms die for loops ersetzen
    @Query("SELECT * FROM wecker WHERE for_me = :forme ORDER BY id")
    androidx.paging.DataSource.Factory<Integer, WeckerPOJO> loadAllWeckersForMe(boolean forme);
    //Alle Wecker
    @Query("SELECT * FROM wecker ORDER BY id")
    LiveData<List<WeckerPOJO>> loadAllWeckers();
    @Query("SELECT * FROM wecker")
    public List<WeckerPOJO> getBootList();




    @Insert
    long insertWecker(WeckerPOJO newWecker);



    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateWecker(WeckerPOJO updated_wecker);
    @Query("UPDATE wecker SET on_off = :onOff WHERE id = :id")
    void turnOnOff(boolean onOff, long id);
    @Query("UPDATE wecker SET shared = :shared WHERE id = :id")
    void updateShared(char shared, long id);
    @Query("UPDATE wecker SET shared_message_con = :newCon WHERE id = :id")
    void updateMessageCon(String newCon, long id);



    @Delete
    void deleteWecker(WeckerPOJO deleted_wecker);
    @Query("DELETE FROM wecker WHERE id = :id")
    void deleteById(long id);
    @Query("DELETE FROM wecker")
    void deleteAll();
}
