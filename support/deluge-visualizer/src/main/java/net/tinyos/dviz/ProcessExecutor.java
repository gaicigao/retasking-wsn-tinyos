package net.tinyos.dviz;

import java.util.Collections;

import java.util.Map;

import net.tinyos.dviz.ProcessExecutor.ProcessResult.Status;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessExecutor {

	public static class ProcessResult {

		public enum Status {
			Success, Failed, Unknown
		};

		private Status status;
		private String output;

		public ProcessResult(Status status, String output) {
			this.status = status;
			this.output = output;
		}

		public Status getStatus() {
			return status;
		}

		public String getOutput() {
			return output;
		}

		@Override
		public String toString() {
			return String.format("Status: (%s)\nOutput: %s", status, output);
		}

	}

	private Map<String, String> envVariables;

	public ProcessExecutor() {

		this(Collections.<String, String> emptyMap());
	}

	public ProcessExecutor(Map<String, String> envVariables) {

		this.envVariables = envVariables;
	}

	private void setEnvVariablesOnlyIfTheyDontExist(
			ProcessBuilder processBuilder) {

		Map<String, String> currentEnvVariables = processBuilder.environment();

		for (Map.Entry<String, String> entry : envVariables.entrySet()) {

			if (currentEnvVariables.containsKey(entry.getKey()) == false) {

				System.out.println(String.format("(%s) not set, value (%s)", entry.getKey(), entry.getValue()));
				currentEnvVariables.put(entry.getKey(), entry.getValue());
			}

		}

	}

	public ProcessResult execute(String[] command, String errorText) {

		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);
		setEnvVariablesOnlyIfTheyDontExist(processBuilder);

		BufferedReader outputFromProcess = null;

		ProcessResult returnValue = new ProcessResult(Status.Unknown, "");

		try {

			Process processFromBuilder = processBuilder.start();
			outputFromProcess = new BufferedReader(new InputStreamReader(
					processFromBuilder.getInputStream()));

			StringBuilder output = new StringBuilder();
			String line;

			while ((line = outputFromProcess.readLine()) != null) {

				// TODO: Should the newline be added back
				output.append(line);
			}

			String outputFromCommand = output.toString();

			if (outputFromCommand.contains(errorText)) {

				returnValue = new ProcessResult(Status.Failed,
						outputFromCommand);
			} else {

				returnValue = new ProcessResult(Status.Success,
						outputFromCommand);
			}

			try {
				processFromBuilder.waitFor();
			} catch (InterruptedException e) {
				returnValue = new ProcessResult(Status.Failed,
						"Command interruped before completing!\n"
								+ outputFromCommand);
			}

		} catch (IOException e) {
			returnValue = new ProcessResult(Status.Failed,
					"IOException while executing command:\n"
							+ toStringStackTrace(e));
		} finally {
			close(outputFromProcess);
		}

		return returnValue;
	}

	private void close(BufferedReader bufferedReader) {

		try {
			if (bufferedReader != null) {
				bufferedReader.close();
			}
		} catch (IOException e) {

			System.err.println("IOException when closing the buffered reader");
		}
	}

	private String toStringStackTrace(Throwable e) {

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}

}
