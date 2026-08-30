package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.EditorialCalendarResponse;

import java.time.LocalDate;

public interface IEditorialCalendarService {
	ApiResponse<EditorialCalendarResponse> getCalendar(LocalDate from, LocalDate to);
}
