
SyntaxInformation[
   lineIntegrate] = {"LocalVariables" -> {"Plot", {3, 3}}, 
   "ArgumentsPattern" -> {_, _, _}};

lineIntegrate[r_?VectorQ, f_Function, {t_, tMin_, tMax_}] := 
 Module[{param, localR}, localR = r /. t -> param;
  Integrate[(f[localR, #] Sqrt[#.#]) &@D[localR, param], {param, tMin,
     tMax}]]

lineIntegrate[{Cos[t], Sin[t]}, 1 &, {t, 0, 2 Pi}]

