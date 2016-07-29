package com.lucaszanella.LattesCrawler;

/**
 * Class to search and load information about people on http://lattes.cnpq.br/. Mainly focused in loading teacher images.
 * You can add any functionality to this if wanted, then send it as a pull request and I'll add here.
 */

import com.lucaszanella.SimpleRequest.SimpleHTTPRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

public class LattesCrawler {
    private SimpleHTTPRequest lattesRequest = new SimpleHTTPRequest();//Lattes only supports HTTP unencrypted connections :(

    private static String protocol = "http";
    private static String domain = "buscatextual.cnpq.br";//page where we search about teachers

    public String magicalRequestPage;
    Boolean debugMode = true;
    Boolean alreadyGotSearchPage = false;

    //Simple request object that will be sent as a response for search("teacherName")
    public class requestObject {
        public String teacherId;//id of teacher in the Lattes system
        public String teacherURLImage;//URL of the teacher curriculum's image
        public String teacherURLAbout;//URL to get more informatiomn about the teacher
        public requestObject(String teachedId, String teacherURLImage, String teacherURLAbout) {
            this.teacherId = teachedId;
            this.teacherURLImage = teacherURLImage;
            this.teacherURLAbout = teacherURLAbout;
        }
    }

    public LattesCrawler() {

    }
    public List < List < String >> getCookies() {
        return this.lattesRequest.getCookies();
    }
    //Just a method to fake that you're entering a search page and typing information (just to prevent them from blocking our web crawler)
    public String getSearchPage() throws Exception{
        URL searchPage = new URL(protocol + "://" + domain + "/" + "buscatextual" + "/" + "busca.do?metodo=apresentar");//URL of the search page
        SimpleHTTPRequest.requestObject lattesSearchPage = lattesRequest.SimpleHTTPRequest(searchPage, null);//Unfortunately it needs to be an insecure HTTP request

        Document doc = Jsoup.parse(lattesSearchPage.response);
        Element formTag = doc.select("form[name=buscaForm]").first();
        magicalRequestPage = formTag.attr("action");//this is a number that I don't know the purpose but I'm using it since the website uses
        alreadyGotSearchPage = true;
        return lattesSearchPage.response;
    }

    public requestObject search(String teacherName) throws Exception {
        if (!alreadyGotSearchPage) {//if didn't get the search page to fake user activity, then get it first
            if (debugMode) {System.out.println("didn't get search page, getting now");};
            getSearchPage();
        }
        if (debugMode) {System.out.println("search called for "+teacherName);};

        //Encode the giant POST query needed to make a search in the Lattes system
        String EMPTY = URLEncoder.encode("", "UTF-8");
        String ZERO = URLEncoder.encode("0", "UTF-8");
        String postQuery = "metodo=" + URLEncoder.encode("buscar", "UTF-8")
                + "&" + "acao=" + EMPTY
                + "&" + "resumoFormacao=" + EMPTY
                + "&" + "resumoAtividade=" + EMPTY
                + "&" + "resumoAtuacao=" + EMPTY
                + "&" + "resumoProducao=" + EMPTY
                + "&" + "resumoPesquisador=" + EMPTY
                + "&" + "resumoIdioma=" + EMPTY
                + "&" + "resumoPresencaDGP=" + EMPTY
                + "&" + "resumoModalidade=" + EMPTY
                + "&" + "modoIndAdhoc=" + EMPTY
                + "&" + "buscaAvancada=" + ZERO
                + "&" + "filtros.buscaNome=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "textoBusca=" + URLEncoder.encode(teacherName, "UTF-8")
                + "&" + "buscarDoutores=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "buscarBrasileiros=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "buscarEstrangeiros=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "paisNascimento=" + ZERO
                + "&" + "textoBuscaTodas=" + EMPTY
                + "&" + "textoBuscaFrase=" + EMPTY
                + "&" + "textoBuscaQualquer=" + EMPTY
                + "&" + "textoBuscaNenhuma=" + EMPTY
                + "&" + "textoExpressao=" + EMPTY
                + "&" + "buscarDoutoresAvancada=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "buscarBrasileirosAvancada=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "buscarEstrangeirosAvancada=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "paisNascimentoAvancada=" + ZERO
                + "&" + "filtros.atualizacaoCurriculo=" + URLEncoder.encode("48", "UTF-8")
                + "&" + "quantidadeRegistros=" + URLEncoder.encode("10", "UTF-8")
                + "&" + "filtros.visualizaEnderecoCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaFormacaoAcadTitCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaAtuacaoProfCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaAreasAtuacaoCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaIdiomasCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaPremiosTitulosCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaSoftwaresCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaProdutosCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaProcessosCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaTrabalhosTecnicosCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaOutrasProdTecCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaArtigosCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaLivrosCapitulosCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaTrabEventosCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaTxtJornalRevistaCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaOutrasProdBibCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaProdArtCultCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaOrientacoesConcluidasCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaOrientacoesAndamentoCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaDemaisTrabalhosCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaDadosComplementaresCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.visualizaOutrasInfRelevantesCV=" + URLEncoder.encode("true", "UTF-8")
                + "&" + "filtros.radioPeriodoProducao=" + URLEncoder.encode("1", "UTF-8")
                + "&" + "filtros.visualizaPeriodoProducaoCV=" + EMPTY
                + "&" + "filtros.categoriaNivelBolsa=" + EMPTY
                + "&" + "filtros.modalidadeBolsa=" + ZERO
                + "&" + "filtros.nivelFormacao=" + ZERO
                + "&" + "filtros.paisFormacao=" + ZERO
                + "&" + "filtros.regiaoFormacao=" + ZERO
                + "&" + "filtros.ufFormacao=" + ZERO
                + "&" + "filtros.nomeInstFormacao=" + EMPTY
                + "&" + "filtros.conceitoCurso=" + EMPTY
                + "&" + "filtros.buscaAtuacao=" + URLEncoder.encode("false", "UTF-8")
                + "&" + "filtros.codigoGrandeAreaAtuacao=" + ZERO
                + "&" + "filtros.codigoAreaAtuacao=" + ZERO
                + "&" + "filtros.codigoSubareaAtuacao=" + ZERO
                + "&" + "filtros.codigoEspecialidadeAtuacao=" + ZERO
                + "&" + "filtros.orientadorCNPq=" + EMPTY
                + "&" + "filtros.idioma=" + ZERO
                + "&" + "filtros.grandeAreaProducao=" + ZERO
                + "&" + "filtros.areaProducao=" + ZERO
                + "&" + "filtros.setorProducao=" + ZERO
                + "&" + "filtros.naturezaAtividade=" + ZERO
                + "&" + "filtros.paisAtividade=" + ZERO
                + "&" + "filtros.regiaoAtividade=" + ZERO
                + "&" + "filtros.ufAtividade=" + ZERO
                + "&" + "filtros.nomeInstAtividade=" + EMPTY;

        URL searchPageToPostTo = new URL(protocol + "://" + domain + "" + magicalRequestPage);//post the search to this page
        SimpleHTTPRequest.requestObject lattesSearchPage = lattesRequest.SimpleHTTPRequest(searchPageToPostTo, postQuery);

        Document doc = Jsoup.parse(lattesSearchPage.response);
        String teacherId;
        String teacherURLImage;
        String teacherURLAbout;
        try {
            Element resultadoTag = doc.getElementsByClass("resultado").first();
            String codeMess = resultadoTag.select("a").first().attr("href");
            teacherId = codeMess.split("\\('")[1].split("'")[0];
            teacherURLImage = "http://servicosweb.cnpq.br/wspessoa/servletrecuperafoto?tipo=1&id="+teacherId;
            teacherURLAbout = "http://buscatextual.cnpq.br/buscatextual/preview.do?metodo=apresentar&id="+teacherId;
        } catch (Exception e) {
            teacherId = null;
            teacherURLAbout = null;
            teacherURLImage = null;
        }
        return new requestObject(teacherId, teacherURLImage, teacherURLAbout);
    }
}