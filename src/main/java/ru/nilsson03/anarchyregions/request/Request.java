package ru.nilsson03.anarchyregions.request;

import java.util.UUID;

public record Request(UUID inviter, UUID target, UUID regionId) {

}
