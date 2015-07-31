package org.loklak.api.client;

import org.loklak.data.DAO;
import org.loklak.harvester.SourceType;

import java.io.File;


public class DeleteUsers {

    private static final String[] ID_USERS = {"NOSK","MTH","POSO","MUUG","JUGI","SITCON","EBUG","JMILUG","PPLUG","PI","JOZILUG","X-LUG","UnPLUG","HANOILUG","JNUG","UJLT","PUGS","CHITS","MM","H0LUG","BLUA","LR","KoreaLUG","UPLUG","FLOSLM","CALLUG","MB","DL","PERLUG","AOSI","MOZ-Kerala","LA","ISFAHANLUG","TAOSC","FOSSCO","OMSKLUG","COSCUP","MLUG","SUSELUG","UQ","AULT","UKT","G0v.tw","JUGM","UCT","PLLUG","LSF","Jenkins-JP","BOSN","UELT","Glug","kalug","IL","STS","PGNU","BLUG","VFOSSA","JOSA","ODC","BF","QGLUG","TLUA","MOZ-HK","TCLUG","KLUGNU","OMM","PSEB","GULCT","OSC","TUSG","UCY","DAIICT","FOSSB","DevNights","MOZSL","JOLUG","MOPCON","MR","NBUG","USALT","FOSSLebaneseMovement","UPLT","ECHO","HBUG","LUGM","BP","NSEC-LUG","UMLT","p-lug","ILUG","JLC","MOSUT","BH","VOLGOGRAD","TLUG","HLUG","PERLMONG","RCN","PYJP","THLC","MGLUG","HKLUG","UILT","Yoko-lug","UBLT","PRB","Fedora Project - Myanmar Community","BITLUG","UTLT","TAFSTG","OSSF","SREC-LUG","Q8L","TAIPEI-LUG","SCJLUG","DLGG","UJT","BANLUG","RV-LUG","PID","TULT","NLUG","TSLMG","URLT","PLWUG","UUAE","DLUG","UIT","HEBLUG","THROR","ILUGChennai","UT","SLUG","KJBUG","PRUG","KBUG","ILUGC","OpenHackTaipei","ILUG-Bom","LP","SHLUG","TU-LUG","HSG","IOR","LINUXJP","FOSSNC","LUGU","SPLUG","IDR","CLUG","Rlug","klug","Hanthana","LB","YGNU","MOSS","CDLug","tossug"};

    public static void main(String[] args) {
        File data = new File(new File("."), "data");
        if (!data.exists()) data.mkdirs();
        File tmp = new File(data, "tmp");
        if (!tmp.exists()) data.mkdirs();
        //DAO.init(data);
        int cnt = 0;
        for (String id : ID_USERS) {
            //if (DAO.deleteUser(id, SourceType.IMPORT)) {
                cnt++;
            //}
        }
        System.out.println("Deleted " + cnt);
    }
}
