package com.alaruss.verbs.models;

import android.text.TextUtils;

import java.util.Date;

public class Verb {
    private int id;
    private String infinitive;
    private String data;
    private String translationEn;
    private String translationEs;
    private Date lastAccess;
    private boolean isFavorite;


    private int accessCount;

    private Tiempo indPresent;
    private Tiempo indImperfet;
    private Tiempo indPassatSimple;
    private Tiempo indFutur;
    private Tiempo indCondicional;

    private Tiempo subPresent;
    private Tiempo subImperfet;
    private Tiempo imperatiu;
    private TiempoCompost comPassatPerifrastic;
    private TiempoCompost comPerfet;
    private TiempoCompost comPlusquamperfet;
    private TiempoCompost comPassatAnterior;
    private TiempoCompost comPassatAnteriorPerifrastic;
    private TiempoCompost comFuturPerfet;
    private TiempoCompost comCondicionalPerfet;
    private TiempoCompost comSubPassatPerifrastic;
    private TiempoCompost comSubPerfet;
    private TiempoCompost comSubPlusquamperfet;
    private TiempoCompost comSubPassaAnteriorPerifrastic;
    private String[] participi;
    private String gerundi;

    public TiempoCompost getComPassatAnterior() {
        return comPassatAnterior;
    }

    public TiempoCompost getComPassatAnteriorPerifrastic() {
        return comPassatAnteriorPerifrastic;
    }

    public TiempoCompost getComFuturPerfet() {
        return comFuturPerfet;
    }

    public TiempoCompost getComCondicionalPerfet() {
        return comCondicionalPerfet;
    }

    public TiempoCompost getComSubPassatPerifrastic() {
        return comSubPassatPerifrastic;
    }

    public TiempoCompost getComSubPlusquamperfet() {
        return comSubPlusquamperfet;
    }

    public TiempoCompost getComSubPassaAnteriorPerifrastic() {
        return comSubPassaAnteriorPerifrastic;
    }


    public Verb() {
    }

    public Tiempo getIndPresent() {
        return indPresent;
    }

    public Tiempo getIndImperfet() {
        return indImperfet;
    }

    public Tiempo getIndPassatSimple() {
        return indPassatSimple;
    }

    public Tiempo getIndFutur() {
        return indFutur;
    }

    public Tiempo getIndCondicional() {
        return indCondicional;
    }

    public Tiempo getSubPresent() {
        return subPresent;
    }

    public Tiempo getSubImperfet() {
        return subImperfet;
    }

    public Tiempo getImperatiu() {
        return imperatiu;
    }

    public String getParticipi() {
        return TextUtils.join("\n", participi);
    }

    public String getGerundi() {
        return gerundi;
    }

    public TiempoCompost getComPassatPerifrastic() {
        return comPassatPerifrastic;
    }

    public TiempoCompost getComPerfet() {
        return comPerfet;
    }

    public TiempoCompost getComPlusquamperfet() {
        return comPlusquamperfet;
    }

    public TiempoCompost getComSubPerfet() {
        return comSubPerfet;
    }

    public class Tiempo {
        protected String[] forms;

        public Tiempo(String data) {
            forms = data.split(",");
            if (forms.length == 0) {
                forms = new String[6];
            }
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (String i : forms) {
                if (i != null) {
                    builder.append(i);
                } else {
                    builder.append("-");
                }
                builder.append("\n");
            }
            return builder.toString();
        }

    }

    public class TiempoCompost extends Tiempo {
        private String base;

        public TiempoCompost(String data, String base) {
            super(data);
            this.base = base;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (String i : forms) {
                builder.append(i).append(" ").append(base).append("\n");
            }
            return builder.toString();
        }
    }

    public int getId() {
        return id;
    }

    public void setInfinitive(String s) {
        this.infinitive = s;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setFavorite(int i) {
        this.isFavorite = i != 0;
    }

    public String getInfinitive() {
        return infinitive;
    }

    public void setData(String data) {
        this.data = data;
        this.parseData();
    }

    public void setTranslationEn(String translation) {
        this.translationEn = translation;
    }

    public void setTranslationEs(String translation) {
        this.translationEs = translation;
    }

    public void parseData() {
        if (data == null) {
            return;
        }
        String[] temps = data.split("\\|");
        participi = temps[0].split(",");
        gerundi = temps[1];
        indPresent = new Tiempo(temps[2]);
        indPassatSimple = new Tiempo(temps[3]);
        indImperfet = new Tiempo(temps[4]);
        indFutur = new Tiempo(temps[5]);
        indCondicional = new Tiempo(temps[6]);
        subPresent = new Tiempo(temps[7]);
        subImperfet = new Tiempo(temps[8]);
        imperatiu = new Tiempo(temps[9]);
        comPerfet = new TiempoCompost("he,has,ha,hem,heu,han", participi[0]);
        comPassatPerifrastic = new TiempoCompost("vaig,vas,va,vam,vau,van", infinitive);
        comPlusquamperfet = new TiempoCompost("havia,havies,havia,havíem,havíeu,havien", participi[0]);
        comPassatAnterior = new TiempoCompost("haguí,hagueres,hagué,haguérem,haguéreu,hagueren", participi[0]);
        comPassatAnteriorPerifrastic = new TiempoCompost("vaig haver,vas haver,va haver,vam haver,vau haver,van haver", participi[0]);
        comFuturPerfet = new TiempoCompost("hauré,hauràs,haurà,haurem,haureu,hauran", participi[0]);
        comCondicionalPerfet = new TiempoCompost("hauria,hauries,hauria,hauríem,hauríeu,haurien", participi[0]);
        comSubPassatPerifrastic = new TiempoCompost("vagi,vagis,vagi,vàgim,vàgiu,vagin", infinitive);
        comSubPerfet = new TiempoCompost("hagi,hagis,hagi,hàgim,hàgiu,hagin", participi[0]);
        comSubPlusquamperfet = new TiempoCompost("hagués,haguessis,hagués,haguéssim,haguéssiu,haguessin", participi[0]);
        comSubPassaAnteriorPerifrastic = new TiempoCompost("vagi haver,vagis haver,vagi haver,vàgim haver,vàgiu haver,vagin haver", participi[0]);
    }

    public String getTranslationEn() {
        if (this.translationEn == null) {
            return "";
        } else {
            return this.translationEn;
        }
    }

    public String getTranslationEs() {
        if (this.translationEs == null) {
            return "";
        } else {
            return this.translationEs;
        }
    }

    public String getData() {
        return data;
    }

    public void setLastAccess(Date date) {
        lastAccess = date;
    }

    public void setLastAccess(int date) {
        lastAccess = new Date((long) date * 1000);
    }

    public Date getLastAccess() {
        return lastAccess;
    }

    public int getLastAccessTS() {
        return (int) (lastAccess.getTime() / 1000);
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public int getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(int accessCount) {
        this.accessCount = accessCount;
    }

}
