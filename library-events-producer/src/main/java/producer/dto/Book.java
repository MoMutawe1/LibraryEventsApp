package producer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Book(@NotNull Integer BookId, @NotBlank String bookName, @NotBlank String bookAuthor) {
}
