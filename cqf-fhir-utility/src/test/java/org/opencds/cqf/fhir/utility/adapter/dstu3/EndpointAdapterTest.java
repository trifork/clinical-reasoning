package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.junit.jupiter.api.Test;

public class EndpointAdapterTest {
    @Test
    void invalid_object_fails() {
        assertThrows(IllegalArgumentException.class, () -> new LibraryAdapter(new PlanDefinition()));
    }

    @Test
    void adapter_get_and_set_address() {
        var endpoint = new Endpoint();
        var address = "123 Test Street";
        endpoint.setAddress(address);
        var adapter = new EndpointAdapter(endpoint);
        assertEquals(address, adapter.getAddress());
        var newAddress = "456 Test Street";
        adapter.setAddress(newAddress);
        assertEquals(newAddress, endpoint.getAddress());
    }
}
