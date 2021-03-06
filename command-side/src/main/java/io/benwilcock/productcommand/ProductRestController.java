package io.benwilcock.productcommand;

import io.benwilcock.productcommand.commands.AddProductCommand;
import io.benwilcock.utils.Asserts;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.repository.ConcurrencyException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * Created by ben on 19/01/16.
 */
@RestController
@RequestMapping
public class ProductRestController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductRestController.class);

    @Autowired
    CommandGateway commandGateway;

    @RequestMapping(value = "/add/{id}", method = RequestMethod.POST)
    public void add(@PathVariable(value = "id") String id,
                    @RequestParam(value = "name", required = true) String name,
                    HttpServletResponse response) {

        LOG.debug("Adding Product [{}] '{}'", id, name);

        try {
            Asserts.INSTANCE.areNotEmpty(Arrays.asList(id, name));
            AddProductCommand command = new AddProductCommand(id, name);
            commandGateway.sendAndWait(command);
            LOG.info("Added Product [{}] '{}'", id, name);
            response.setStatus(HttpServletResponse.SC_CREATED);// Set up the 201 CREATED response
            return;
        } catch (AssertionError ae) {
            LOG.error("Add Request failed - empty params?. [{}] '{}'", id, name);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (CommandExecutionException cex) {
            LOG.error("Add Command FAILED with Message: {}", cex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            if (null != cex.getCause()) {
                Throwable cexCause = cex.getCause();
                LOG.error("Caused by: {} {}", cexCause.getClass().getName(), cexCause.getMessage(), cexCause);
                if (cexCause instanceof ConcurrencyException
                        || cexCause instanceof ConstraintViolationException
                        || cexCause instanceof PersistenceException) {
                    LOG.error("A duplicate product with the same ID [{}] already exists.", id);
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                }
            }
        }
    }
}
