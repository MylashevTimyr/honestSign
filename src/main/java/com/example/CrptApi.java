package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class CrptApi {
	private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
	private final Semaphore semaphore;
	private final int requestsLimit;
	private final long interval;

	public CrptApi(int requestsLimit, long interval) {
		this.requestsLimit = requestsLimit;
		this.interval = interval;
		this.semaphore = new Semaphore(requestsLimit);
	}

	public void createDocument(Document document, String signature) {
		try {
			semaphore.acquire();

			try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
				HttpPost httpPost = new HttpPost(API_URL);
				httpPost.setHeader("Content-Type", "application/json");

				ObjectMapper objectMapper = new ObjectMapper();
				String documentJson = objectMapper.writeValueAsString(document);

				String requestBody = String.format("{ \"product_document\": \"%s\", \"document_format\": \"MANUAL\", \"type\": \"LP_INTRODUCE_GOODS\", \"signature\": \"%s\" }", documentJson, signature);
				StringEntity entity = new StringEntity(requestBody);
				httpPost.setEntity(entity);

				try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
					int statusCode = response.getStatusLine().getStatusCode();
					if (statusCode == 200) {
						System.out.println("Document successfully created!");
					} else {
						System.out.println("Failed to create the document. Status code: " + statusCode);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			semaphore.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		CrptApi crptApi = new CrptApi(3, 1000); // Ограничение - 3 запроса в секунду

		List<Thread> threads = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			final int documentNumber = i + 1;
			Thread thread = new Thread(() -> {
				Document.Description description = new Document.Description("participantInn");
				Document.Product product1 = new Document.Product("certificate 1", "2024-05-28", "AA111", "owner1", "producer1", "2024-05-25", "tnved1", "uit1", "uitu1");
				Document.Product product2 = new Document.Product("certificate 2", "2023-04-29", "BB222", "owner2", "producer2", "2023-04-24", "tnved2", "uit2", "uitu2");

				List<Document.Product> products = new ArrayList<>();
				products.add(product1);
				products.add(product2);

				Document document = new Document(description, "1", "approved", "LP_INTRODUCE_GOODS", true, "owner_inn", "participant_inn", "producer_inn", "2024-07-04", "production_type", products, "2024-07-04", "reg123");
				String signature = "Подпись";
				crptApi.createDocument(document, signature + " " + documentNumber);
			});
			threads.add(thread);
			thread.start();
		}

		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Document {
		private Description description;
		private String doc_id;
		private String doc_status;
		private String doc_type;
		private boolean importRequest;
		private String owner_inn;
		private String participant_inn;
		private String producer_inn;
		private String production_date;
		private String production_type;
		private List<Product> products;
		private String reg_date;
		private String reg_number;

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		public static class Description {
			private String participantInn;
		}

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		public static class Product {
			private String certificate_document;
			private String certificate_document_date;
			private String certificate_document_number;
			private String owner_inn;
			private String producer_inn;
			private String production_date;
			private String tnved_code;
			private String uit_code;
			private String uitu_code;
		}
	}
}
