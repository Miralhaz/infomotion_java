//package org.example.classesWillian;
//import org.example.AwsConnection;
//import org.example.Connection;
//
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//
//public class TratamentoJsonWillian {
//    //Declarando classes utilizadas
//    AwsConnection aws = new AwsConnection();
//
//    //dados que preciso pegar do csv logs_consolidados+servidores.csv
//    public Integer fk_servidor;
//    public Double fk_Disco;
//    public String nomeMaquina;
//    public Double disco;
//    public Double temperatura_disco;
//    public Integer quantidade_processos;
//    public DateTimeFormatter timestamp;
//    //dados leitura para ver os que mais operam
//    public Double numero_leituras;
//    public Double numero_escritas;
//    public Double  bytes_lidos;
//    public Double bytes_escritos;
//    public Double tempo_leitura;
//    public Double tempo_escrita;
//
//    public List<TratamentoJsonWillian> ListaTop5DiscosMaisCheios = new ArrayList<>();
//    public List<TratamentoJsonWillian> KpiNumeroDeAlertasOntemEHoje = new ArrayList<>();
//    public List<TratamentoJsonWillian> KpiTop3DiscoGravandoMais = new ArrayList<>();
//    public List<TratamentoJsonWillian> KpiTop3DiscosMaiorTempertura = new ArrayList<>();
//    // cada um desses gráficos terão 8 pontos de plotagem no gráfico
//    // cada um deles pegado com metodo ajustado
//    public List<TratamentoJsonWillian> GraficoHistoricoUmdia  = new ArrayList<>();
//    public List<TratamentoJsonWillian> GraficoHistoricoSeteDias = new ArrayList<>();
//    public List<TratamentoJsonWillian> GraficoHistoricoUmMes = new ArrayList<>();
//    //Todos os id's para criar json no client para todas as empresa
//    ArrayList<Integer> idEmpresas = new ArrayList<>();
//
//
//    // Contrutores
//
//    public TratamentoJsonWillian(Integer fk_servidor, Double fk_Disco, String nomeMaquina, Double disco, Double temperatura_disco, Integer quantidade_processos, DateTimeFormatter timestamp,
//                                 Double numero_leituras, Double numero_escritas, Double bytes_lidos, Double bytes_escritos, Double tempo_leitura, Double tempo_escrita,
//                                 List<TratamentoJsonWillian> listaTop5DiscosMaisCheios, List<TratamentoJsonWillian> kpiNumeroDeAlertasOntemEHoje,
//                                 List<TratamentoJsonWillian> kpiTop3DiscoGravandoMais, List<TratamentoJsonWillian> kpiTop3DiscosMaiorTempertura, List<TratamentoJsonWillian> graficoHistoricoUmdia, List<TratamentoJsonWillian> graficoHistoricoSeteDias, List<TratamentoJsonWillian> graficoHistoricoUmMes) {
//        this.fk_servidor = fk_servidor;
//        this.fk_Disco = fk_Disco;
//        this.nomeMaquina = nomeMaquina;
//        this.disco = disco;
//        this.temperatura_disco = temperatura_disco;
//        this.quantidade_processos = quantidade_processos;
//        this.timestamp = timestamp;
//        this.numero_leituras = numero_leituras;
//        this.numero_escritas = numero_escritas;
//        this.bytes_lidos = bytes_lidos;
//        this.bytes_escritos = bytes_escritos;
//        this.tempo_leitura = tempo_leitura;
//        this.tempo_escrita = tempo_escrita;
//        ListaTop5DiscosMaisCheios = listaTop5DiscosMaisCheios;
//        KpiNumeroDeAlertasOntemEHoje = kpiNumeroDeAlertasOntemEHoje;
//        KpiTop3DiscoGravandoMais = kpiTop3DiscoGravandoMais;
//        KpiTop3DiscosMaiorTempertura = kpiTop3DiscosMaiorTempertura;
//        GraficoHistoricoUmdia = graficoHistoricoUmdia;
//        GraficoHistoricoSeteDias = graficoHistoricoSeteDias;
//        GraficoHistoricoUmMes = graficoHistoricoUmMes;
//    }
//
//    // metodos
//
//    Connection connection = new Connection();
//
//    public Connection getConnection() {
//        return connection;
//    }
//
//    public List<TratamentoJsonWillian> PegarApelidoDisco(Integer fk_servidor){
//        List<TratamentoJsonWillian> discoLidoJson = new ArrayList<>();
//        Integer recebeIdComponenteDisco = 0;
//        String selectNomeDisco = "select\n" +
//                "c.id,\n" +
//                "c.apelido\n" +
//                "from servidor as s\n" +
//                "inner join componentes as c on s.id = c.fk_servidor\n" +
//                "where c.tipo = \"DISCO\";";
//
//        PegarParametroeEspecificacoes(recebeIdComponenteDisco);
//        return discoLidoJson;
//    }
//
//    public Double PegarParametroeEspecificacoes(Integer fkComponente){
//        Double receberParametroSelect = 52.0;
//        // estou pegando o parametro para enviar junto ao json de cada disco
//        String ParametroeEspecificacoes = "select\n" +
//                "c.apelido,\n" +
//                "valor\n" +
//                "from especificacao_componente as e\n" +
//                "inner join componentes as c on c.id = e.fk_componente\n" +
//                "where c.tipo = \"DISCO\" and e.fk_componente = ?;";
//        // remover o parametro 52 default para o que achar receberParametroSelect
//
//        return receberParametroSelect;
//    }
//
//    public List<Integer> PegarTodosIdsEmpresa(Integer fkServidor){
//
//        // estou pegando o id da empresa para colocar no nome do arquivo quando for organizar
//        String IdEmpresa = "select\n" +
//                "e.id\n" +
//                "from empresa as e\n" +
//                "inner join servidor as s on s.fk_empresa = e.id;";
//        // remover o parametro 52 default para o que achar receberParametroSelect
//        //setar o id recebido de Empresa
//        idEmpresas.add(10);
//        return idEmpresas;
//    }
//
//    public void criarJsonEmpresas(){
//        for (int i = 0; i < idEmpresas.size(); i++) {
//            int idEmpresa = idEmpresas.get(i);
//            // criar json de nome DiscoTratamentoIdEmpresa
//            aws.uploadBucketClient("TratamentosWillian", "DiscoTratamentoIdEmpresa");
//        }
//    }
//
//    public void SepararLinhasJsonGeralPorEmpresa(){
//        // apos transformar o csv do bucket trusted em json,
//        // fazer essa separação pelos arquivos
//    }
//
//    public void AcessarJsoIndividualemente(){
//        // todos os arquivos que estiverem no client na pasta TratamentosWillian
//        // receberão o mesmo tratamento individual para em todas as linhas
//    }
//
//
//
//
//}
