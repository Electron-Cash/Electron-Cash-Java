package main;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.BeforeClass;

import org.json.me.JSONArray;
import org.json.me.JSONObject;

import electrol.java.util.ArrayList;
import electrol.java.util.HashMap;
import electrol.java.util.Iterator;


import main.electron.Address;
import main.electron.Base58;
import main.Bitcoin;
import main.Util;
import main.Transaction;
import java.math.BigInteger;

public class TestWallet{

    @Test
    public void Test_001_load_empty(){
      boolean creationExceptionThrown = false;
      // Make new empty storafe for wallet
      Storage s = new Storage("empty_wallet");
      // Making wallet with empty storage
      try {
        Wallet w = new Wallet(s);
      } catch (Exception e){
        creationExceptionThrown = true;
      }

      assertFalse(creationExceptionThrown);

    }

    @Test
    public void test_002_load_non_empty(){
      boolean creationExceptionThrown = false;
      Storage s = new Storage("my_wallet.json");
      String[] receivingAddresses = {
        "1DM5KiTe7LQHJTLNhhJMq8UYeZmYZYDrxH",
        "1NGeDQ4NDDZ9znuoyqMK6Zy2QgcsTanUWV",
        "1FbQaj74iFus9tEuxLirZv2iom3iJ3dkRh"
      };
      String[] changeAddresses = {
        "1LkdqN4w4f5PwYux7uUq5VRdicR8ZbfQsd",
        "1PjkVcVYpuLVFDR1N4FCV5y8Q6Hn8tgyMR",
        "1KantuKDYfFaJRj5wDWXzQj1jAyndwf5eN"
      };
      Address someAddress = null;
      Address foreingAddress = null;
      try{
        someAddress = Address.from_string("1PjkVcVYpuLVFDR1N4FCV5y8Q6Hn8tgyMR");
        foreingAddress = Address.from_string("17B2nADwn4pXbbd92Ds2VnZ64B5zEr28sH");
      } catch(Exception ex){
        assertFalse("Failed to make address from string" , true);
      }
      try {
        Wallet w = new Wallet(s);
        ArrayList receivingAddressesWallet = w.getReceiveingAddresses();
        ArrayList changeAddressesWallet = w.getChangeAddresses();
        ArrayList allAddressesWallet = w.getAddresses();
        ArrayList rStrings = new ArrayList();
        ArrayList cStrings = new ArrayList();
        ArrayList aStrings = new ArrayList();
        for(int i = 0; i < receivingAddressesWallet.size(); i++){
          rStrings.add(((Address)receivingAddressesWallet.get(i)).toString((byte)1));
        }
        for(int i = 0; i < changeAddressesWallet.size(); i++){
          cStrings.add(((Address)changeAddressesWallet.get(i)).toString((byte)1));
        }
        for(int i = 0; i < allAddressesWallet.size(); i++){
          aStrings.add(((Address)allAddressesWallet.get(i)).toString((byte)1));
        }
        for(String receivingAddress:receivingAddresses){
          assertTrue("Receiving addresses should contains test receiving address", rStrings.contains(receivingAddress));
          assertTrue("All addresses should contains test receiving address", aStrings.contains(receivingAddress));
        }
        for(String changeAddress:changeAddresses){
          assertTrue("Change addresses should contains test change address", cStrings.contains(changeAddress));
          assertTrue("All addresses should contains test change address", aStrings.contains(changeAddress));
        }
        assertTrue("test address is mine", w.isMine(someAddress));
        assertFalse("test address is not mine", w.isMine(foreingAddress));
        // Test making tx
        HashMap output1 = new HashMap();
        output1.put("type", 0);
        output1.put("address", Address.from_string("1CrJ7wXVyyxNFm3Sk1f3c3cksjPD86MQsa"));
        output1.put("value", 1000L);
        HashMap output2 = new HashMap();
        output2.put("type", 0);
        output2.put("address", Address.from_string("1HX6VgfAKgY2J37eaNgBTLNcfUTorTvDq4"));
        output2.put("value", 1000L);
        HashMap output3 = new HashMap();
        output3.put("type", 0);
        output3.put("address", Address.from_string("12Pv5Gn9miZnfdb9vyXqcUqzREftnGe1q8"));
        output3.put("value", 1000L);
        ArrayList outputs = new ArrayList();
        outputs.add(output1);
        outputs.add(output2);
        outputs.add(output3);

        HashMap config = new HashMap();
        w.mktx(outputs, "", config, 0L, null, null);
        //
        Address addrForCheckingHistory = Address.from_string("1ocdXFykf8bGfZDVThJDgBppBCbrGsZ3Q");
        w.getAddressHistory(addrForCheckingHistory);
        //
        HashMap res = w.getAddrIo(addrForCheckingHistory);
        assertTrue(((HashMap)res.get("received")).containsKey("43987285aa84d576878cad3a33bbb46aec4fdbb21dab3a105e7e6b4c577e271d:0"));
        HashMap received = ((HashMap)
                            ((HashMap)res.get("received"))
                            .get("43987285aa84d576878cad3a33bbb46aec4fdbb21dab3a105e7e6b4c577e271d:0"));
        assertEquals( 505305L, received.get("height"));
        assertEquals( 5000L, received.get("v"));
        assertEquals( false, received.get("is_cb"));
        assertTrue(((HashMap)res.get("sent")).containsKey("43987285aa84d576878cad3a33bbb46aec4fdbb21dab3a105e7e6b4c577e271d:0"));
        long sent = (long)((HashMap)res.get("sent")).get("43987285aa84d576878cad3a33bbb46aec4fdbb21dab3a105e7e6b4c577e271d:0");
        assertEquals( 505489L, sent);
        // Check for getAddrUtxo
        Address addrForTest = Address.from_string("1HdGRAJjzsPZrVrJfkFRMjM3jCib7viZgD");
        res = w.getAddrUtxo(addrForTest);
        assertTrue(res.containsKey("d5fa351d731f4c8248beed455663afe0a8b7e4c69180e8e9bda2e9d9ef876493:0"));
        HashMap utxo = (HashMap)res.get("d5fa351d731f4c8248beed455663afe0a8b7e4c69180e8e9bda2e9d9ef876493:0");
        assertEquals("d5fa351d731f4c8248beed455663afe0a8b7e4c69180e8e9bda2e9d9ef876493", utxo.get("prevout_hash"));
        assertEquals(506451L, utxo.get("height"));
        assertEquals(false, utxo.get("coinbase"));
        assertEquals(addrForTest, utxo.get("address"));
        assertEquals(1000L, utxo.get("value"));
        assertEquals(0L, utxo.get("prevout_n"));
      } catch (Exception e){
        creationExceptionThrown = true;
      }
      assertFalse("EXCEPTION IN A TEST THREAD!", creationExceptionThrown);
    }



    @Test
    public void someTest(){
        String raw = "010000000221770a377ce8810365eb38ec86e152f62aab697fb1559eb2b49adb8e625ce8a2010000006a47304402206baaadb98e9d0f1a9f24ddad1bcc4f4cc91469a328faa9c53f6ee9df57e54ad4022055ad8a4cc1f7c690d516ae68386f0aa15c86b49bb9a70adf3d2bc6e094ebd6144121026b2e6e3faae0e21072e6802fda731f6f7bd50b7db15a0c439614834277d0d0fafeffffff21770a377ce8810365eb38ec86e152f62aab697fb1559eb2b49adb8e625ce8a2000000006b4830450221008de4730466ce9d9ba56a5b835290a5511172c3f57daf247915de0d05d65026fd02203d4bc107029651e1fbcebe8257f5ec26b0b0cc8ac7f7384da2c8e19ce420c1d141210333f46e6db3a03ce224226aae67e4387c4e308ca8a5b8a0e8acc492ad1a263c71feffffff04e8030000000000001976a914382656a66b3dab8939f0df7fd45ceff85772ffe188ace8030000000000001976a914bba79a234c1b943df25d941657148ad6a78ce6ec88acd0070000000000001976a91436152ee22e9ffc72648c70c5ea464ac91473d2a988acd0070000000000001976a914d8ab060a7a3a05ced0d96f5ca8f8e05929422a2088ac00000000";
        String[] prevout_hashes = {
          "a2e85c628edb9ab4b29e55b17f69ab2af652e186ec38eb650381e87c370a7721",
          "a2e85c628edb9ab4b29e55b17f69ab2af652e186ec38eb650381e87c370a7721"
        };
        long[] prevout_ns = new long[]{1,0};
        long[] sequences = new long[]{4294967294L, 4294967294L};
        String[][] signatures = {
          {"304402206baaadb98e9d0f1a9f24ddad1bcc4f4cc91469a328faa9c53f6ee9df57e54ad4022055ad8a4cc1f7c690d516ae68386f0aa15c86b49bb9a70adf3d2bc6e094ebd61441"},
          {"30450221008de4730466ce9d9ba56a5b835290a5511172c3f57daf247915de0d05d65026fd02203d4bc107029651e1fbcebe8257f5ec26b0b0cc8ac7f7384da2c8e19ce420c1d141"}
        };
        String[][] pubkeys = {
          {"026b2e6e3faae0e21072e6802fda731f6f7bd50b7db15a0c439614834277d0d0fa"},
          {"0333f46e6db3a03ce224226aae67e4387c4e308ca8a5b8a0e8acc492ad1a263c71"}
        };
        String[] scriptSigs = {
          "47304402206baaadb98e9d0f1a9f24ddad1bcc4f4cc91469a328faa9c53f6ee9df57e54ad4022055ad8a4cc1f7c690d516ae68386f0aa15c86b49bb9a70adf3d2bc6e094ebd6144121026b2e6e3faae0e21072e6802fda731f6f7bd50b7db15a0c439614834277d0d0fa",
          "4830450221008de4730466ce9d9ba56a5b835290a5511172c3f57daf247915de0d05d65026fd02203d4bc107029651e1fbcebe8257f5ec26b0b0cc8ac7f7384da2c8e19ce420c1d141210333f46e6db3a03ce224226aae67e4387c4e308ca8a5b8a0e8acc492ad1a263c71"
        };
        ArrayList outputs = new ArrayList();
        HashMap output = new HashMap();
        output.put("type",0);
        output.put("address","167tmYhgHmiP5aUh3hXPxbKN1DFuks81zX");
        output.put("value", BigInteger.valueOf(1000));
        outputs.add(output);
        output = new HashMap();
        output.put("type", 0);
        output.put("address", "1J7E955v4RmibZY9TaEAiEhQn8ULRz2ewu");
        output.put("value", BigInteger.valueOf(1000));
        outputs.add(output);
        output = new HashMap();
        output.put("type", 0);
        output.put("address", "15vxsByDwv5ZFjPVkWuLttgnj2b28XLkwm");
        output.put("value", BigInteger.valueOf(2000));
        outputs.add(output);
        output = new HashMap();
        output.put("type", 0);
        output.put("address", "1LkdqN4w4f5PwYux7uUq5VRdicR8ZbfQsd");
        output.put("value", BigInteger.valueOf(2000));
        outputs.add(output);

        int[] num_sigs = {1,1};
        String[] addresses = {"1ChQDCrcHbzC26MUAJ8krekyn1JH6SG8Qv","1Dg8yjuREx8HTrurNJXNdDgHE6t8HPmPEv"};
        HashMap tx = new HashMap();
        try{
          tx = Transaction.deserialize(raw);
        } catch (Exception ex) {
          assertTrue(false);
        }
        ArrayList inputs =(ArrayList) tx.get("inputs");
        for(int i =0 ; i < inputs.size() ;i++){
          HashMap input = (HashMap) inputs.get(i);
          assertEquals(prevout_hashes[i].toUpperCase(), input.get("prevout_hash"));
          assertEquals(prevout_ns[i], input.get("prevout_n"));
          assertEquals(sequences[i], input.get("sequence"));
          String[] signaturesFromInput = (String[])input.get("signatures");
          for(int j = 0; j < signaturesFromInput.length; j++ ){
            assertEquals(signatures[i][j].toUpperCase(), signaturesFromInput[j]);
          }
          String[] pubkeysFromInput = (String[])input.get("pubkeys");
          for(int j = 0; j < pubkeysFromInput.length; j++ ){
            assertEquals(pubkeys[i][j].toUpperCase(), pubkeysFromInput[j]);
          }
          String scriptSigsFromInput = (String)input.get("scriptSig");
          assertEquals(scriptSigs[i].toUpperCase(), scriptSigsFromInput);
          Address addressFromInput = (Address) input.get("address");
          String addressStr = "";
          try {
            addressStr = addressFromInput.toString((byte)1);
          } catch(Exception e) {
            assertTrue(false);
          }
          assertEquals(addresses[i], addressStr);
          assertEquals(num_sigs[i], input.get("num_sig"));
          assertEquals("p2pkh",input.get("type"));
        }
        ArrayList outputsFromTx = (ArrayList) tx.get("outputs");
        for(int j = 0; j < outputs.size(); j++){
          HashMap outputFromTx = (HashMap)outputsFromTx.get(j);
          int type = (int) outputFromTx.get("type");
          BigInteger val = (BigInteger) outputFromTx.get("value");
          String addr = "";
          try{
            addr = ((Address)outputFromTx.get("address")).toString((byte)1);
          } catch (Exception e) {
            assertTrue(false);
          }
          assertEquals(((HashMap)outputs.get(j)).get("value"), val);
          assertEquals(((HashMap)outputs.get(j)).get("address"), addr);
          assertEquals(((HashMap)outputs.get(j)).get("type"), type);
        }
        assertEquals(1, tx.get("version"));
    }
}
