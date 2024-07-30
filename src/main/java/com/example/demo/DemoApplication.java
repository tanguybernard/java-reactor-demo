package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

//TUto : https://www.invivoo.com/reactor-programmation-non-bloquante/
@SpringBootApplication
public class DemoApplication implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

	private static final WebClient httpClient = WebClient.create(
			"https://data.opendatasoft.com/api/explore/v2.1/catalog/datasets/donnees-hospitalieres-covid-19-dep-france@public/records");

	public static void main(final String[] args) {
		var app = new SpringApplication(DemoApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);// Pour ne pas lancer le serveur web
		app.run(args);
	}

	@Override
	public void run(final String... args) {
		// Notre code à exécuter ici

		log.info("This is an info message");

		// https://data.opendatasoft.com/api/explore/v2.1/catalog/datasets/donnees-hospitalieres-covid-19-dep-france@public/records?limit=20&refine=date%3A2021-09-07&refine=dep_code%3A95&timezone=Europe%2FParis

		String date = "2021-09-07";
		String deuxjourAvantDate = "2021-09-05";
		String source = "OPENDATASOFT";

		final Mono<Integer> mono = Mono.zip(getNumber(date), getNumber(deuxjourAvantDate))
		.doFirst(() -> log.info("Récupération des données entre hier et avant-hier..."))
		.map(tuple -> tuple.getT1() - tuple.getT2())
		.onErrorResume(
			error -> Mono.fromRunnable(() -> log.error("Problème lors de la récupération des données: {}", error.getMessage()))); //si erreur

		var res = mono.block();

		log.info(""+res);

	}

	public Mono<Integer> getNumber(String date) {

		return httpClient.get()
				.uri(uri -> uri.path("")
						.queryParam("refine", "date:" + date, "dep_code:95", "sex:Tous")
						.queryParam("timezone", "Europe/Paris")
						.build()

				)

				.retrieve()
				.bodyToMono(JsonNode.class)
				//Avant l'exec de la requete, on log	
				.doFirst(() -> log.info("Récupération des données pour le {} ...", date))
				//Juste apres on log
				.doOnNext(jsonData -> log.info("Données reçues: {}", jsonData.toPrettyString()))
				.map(jsonData -> jsonData.get("results"))// le reultat se trouve dans ce champs, on le recupere
				.flatMapMany(Flux::fromIterable) // Convertir Mono<List<JsonObject>> en Flux<JsonObject>
				.next() // Récupérer le premier élément du Flux
				.doOnNext(jsonData -> log.info("Premier element de l'attribut results: {}", jsonData.toPrettyString()))
				// .map(data -> data.get("tot_out").intValue())//le map ne gere pas le null donc
				// on va utiliser flatMap plutot
				.flatMap(data -> Mono.justOrEmpty(data.get("tot_out")))
				.map(JsonNode::intValue)
				.switchIfEmpty(Mono.error(new RuntimeException(String.format("Pas de données pour le %s !", date))))//si vide
				;

	}

}

/*
 * 
 * 
 * 
 * .subscribe() va déclencher le flux de manière asynchrone et non-bloquante, et
 * le retour de cette méthode sera donc immédiat.
 * Voici un exemple qui va d’abord afficher Hello ! puis ensuite Valeur reçue :
 * void run() {
 * mono.doOnNext(value -> log.info("Valeur reçue")).subscribe();
 * log.info("Hello !");
 * }
 * 
 * .block() va déclencher le flux, mais le retour de cette méthode sera
 * synchrone, c’est à dire lorsque le flux sera terminé.
 * Comme son nom l’indique, elle va bloquer la méthode run() jusqu’à ce que le
 * Mono se complète.
 * Voici un exemple qui va d’abord afficher Valeur reçue puis ensuite Hello ! :
 * 
 * void run() {
 * var value = mono.doOnNext(value -> log.info("Valeur reçue")).block();
 * log.info("Hello !");
 * }
 * 
 * 
 */