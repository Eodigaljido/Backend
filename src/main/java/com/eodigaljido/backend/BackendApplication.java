package com.eodigaljido.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(BackendApplication.class, args);
	}

	private static void loadDotEnv() {
		Path envFile = Paths.get(".env");
		if (!Files.exists(envFile)) return;

		try {
			Files.lines(envFile)
					.map(String::trim)
					.filter(line -> !line.isBlank() && !line.startsWith("#") && line.contains("="))
					.forEach(line -> {
						int idx = line.indexOf('=');
						String key = line.substring(0, idx).trim();
						String value = line.substring(idx + 1).trim();

						// 인라인 주석 제거 (예: value # comment)
						int commentIdx = value.indexOf(" #");
						if (commentIdx >= 0) value = value.substring(0, commentIdx).trim();

						// 이미 환경변수로 설정된 경우 덮어쓰지 않음
						if (System.getProperty(key) == null && System.getenv(key) == null) {
							System.setProperty(key, value);
						}
					});
		} catch (IOException e) {
			System.err.println("[.env] 파일 로드 실패: " + e.getMessage());
		}
	}
}
