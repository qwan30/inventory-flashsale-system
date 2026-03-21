import { expect } from "vitest";
import * as matchers from "@testing-library/jest-dom/matchers";
import { afterEach, vi } from "vitest";

expect.extend(matchers);

afterEach(() => {
  vi.restoreAllMocks();
});
